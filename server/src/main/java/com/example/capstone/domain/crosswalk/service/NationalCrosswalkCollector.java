package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import com.example.capstone.domain.crosswalk.service.sync.PublicDataCollector;
import com.example.capstone.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class NationalCrosswalkCollector implements PublicDataCollector {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int RAW_RESPONSE_LOG_LIMIT = 3_000;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final CrosswalkRepository crosswalkRepository;

    @Value("${public-data.crosswalk.base-url}")
    private String baseUrl;

    @Value("${public-data.crosswalk.app-key}")
    private String appKey;

    @Value("${public-data.crosswalk.default-page-size}")
    private int pageSize;

    @Value("${public-data.crosswalk.ctprvn-nm:}")
    private String ctprvnNm;

    @Value("${public-data.crosswalk.signgu-nm:}")
    private String signguNm;

    @Override
    public void collectAndSave() {
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while ((pageNo - 1) * pageSize < totalCount) {
            JsonNode body = requestPage(pageNo);

            JsonNode header = body.path("response").path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMsg = header.path("resultMsg").asText();

            CrosswalkErrorCode mappedError = CrosswalkErrorCode.fromOpenApiCode(resultCode);
            if (mappedError != null) {
                if (mappedError == CrosswalkErrorCode.CROSSWALK_NO_DATA) {
                    log.error(
                            "[NATIONAL CROSSWALK COLLECT] unexpected no data. pageNo={}, code={}, msg={}",
                            pageNo,
                            resultCode,
                            resultMsg
                    );
                    throw new BusinessException(CrosswalkErrorCode.CROSSWALK_NO_DATA);
                }

                log.error("[NATIONAL CROSSWALK COLLECT] open api error. code={}, msg={}", resultCode, resultMsg);
                throw new BusinessException(mappedError);
            }

            JsonNode bodyNode = body.path("response").path("body");
            totalCount = bodyNode.path("totalCount").asInt(0);

            JsonNode items = extractItems(bodyNode);

            if (isEmptyItems(items)) {
                log.warn(
                        "[NATIONAL CROSSWALK COLLECT] empty items. pageNo={}, totalCount={}, bodyNode={}",
                        pageNo,
                        totalCount,
                        shrink(bodyNode.toString())
                );

                if (pageNo == 1 && totalCount > 0) {
                    throw new BusinessException(CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE);
                }

                break;
            }

            int savedCount = 0;

            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (upsertCrosswalk(item)) {
                        savedCount++;
                    }
                }
            } else {
                if (upsertCrosswalk(items)) {
                    savedCount++;
                }
            }

            log.info(
                    "[NATIONAL CROSSWALK COLLECT] pageNo={}, pageSize={}, saved={}, totalCount={}",
                    pageNo, pageSize, savedCount, totalCount
            );

            pageNo++;
        }
    }

    private JsonNode extractItems(JsonNode bodyNode) {
        JsonNode items = bodyNode.path("items");

        if (items.isArray()) {
            return items;
        }

        JsonNode item = items.path("item");
        if (item.isArray() || item.isObject()) {
            return item;
        }

        return item;
    }

    private boolean isEmptyItems(JsonNode items) {
        return items.isMissingNode()
                || items.isNull()
                || (items.isArray() && items.isEmpty())
                || (items.isTextual() && !hasText(items.asText()));
    }

    private String shrink(String value) {
        if (value == null) {
            return null;
        }

        return value.length() > RAW_RESPONSE_LOG_LIMIT
                ? value.substring(0, RAW_RESPONSE_LOG_LIMIT)
                : value;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delayMillis = switch (attempt) {
                case 1 -> 1_000L;
                case 2 -> 3_000L;
                case 3 -> 5_000L;
                default -> 10_000L;
            };

            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR);
        }
    }

    private JsonNode requestPageInternal(int pageNo, int numOfRows) {
        String uri = buildUri(pageNo, numOfRows);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", "curl/8.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            int status = response.statusCode();
            String body = response.body();

            if (status >= 400 && status < 500) {
                log.error("[NATIONAL CROSSWALK 4XX] uri={}, status={}, body={}",
                        maskUri(uri), status, shrink(body));
                throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_4XX);
            }

            if (status >= 500) {
                log.error("[NATIONAL CROSSWALK 5XX] uri={}, status={}, body={}",
                        maskUri(uri), status, shrink(body));
                throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_5XX);
            }

            if (body == null || body.isBlank()) {
                throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_EMPTY_BODY);
            }

            log.info("[NATIONAL CROSSWALK RAW RESPONSE] pageNo={}, response={}",
                    pageNo, shrink(body));

            return objectMapper.readTree(body);

        } catch (HttpTimeoutException e) {
            log.error("[NATIONAL CROSSWALK TIMEOUT] uri={}", maskUri(uri), e);
            throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_TIMEOUT);

        } catch (JsonProcessingException e) {
            log.error("[NATIONAL CROSSWALK PARSE ERROR] uri={}", maskUri(uri), e);
            throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_DECODING_ERROR);
        } catch (IOException e) {
            log.error("[NATIONAL CROSSWALK CONNECTION ERROR] uri={}", maskUri(uri), e);
            throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
        }
    }

    private boolean isRetryable(BusinessException e) {
        return CrosswalkErrorCode.CROSSWALK_API_TIMEOUT.getCode().equals(e.getCode())
                || CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR.getCode().equals(e.getCode())
                || CrosswalkErrorCode.CROSSWALK_API_HTTP_5XX.getCode().equals(e.getCode())
                || CrosswalkErrorCode.CROSSWALK_API_EMPTY_BODY.getCode().equals(e.getCode())
                || CrosswalkErrorCode.CROSSWALK_API_ERROR.getCode().equals(e.getCode())
                || CrosswalkErrorCode.CROSSWALK_NO_DATA.getCode().equals(e.getCode());
    }

    private JsonNode requestPage(int pageNo) {
        int maxRetry = 5;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                return requestPageInternal(pageNo, pageSize);
            } catch (BusinessException e) {
                if (!isRetryable(e) || attempt == maxRetry) {
                    throw e;
                }

                log.warn(
                        "[NATIONAL CROSSWALK RETRY] pageNo={}, attempt={}/{}, code={}",
                        pageNo,
                        attempt,
                        maxRetry,
                        e.getCode()
                );

                sleepBeforeRetry(attempt);
            } catch (Exception e) {
                if (attempt == maxRetry) {
                    log.error("[NATIONAL CROSSWALK RETRY FAIL] pageNo={}, attempt={}", pageNo, attempt, e);
                    throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
                }

                log.warn(
                        "[NATIONAL CROSSWALK RETRY] pageNo={}, attempt={}/{}, cause={}",
                        pageNo,
                        attempt,
                        maxRetry,
                        e.getClass().getSimpleName()
                );

                sleepBeforeRetry(attempt);
            }
        }

        throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR);
    }

    private String buildUri(int pageNo, int numOfRows) {
        String encodedServiceKey = URLEncoder.encode(appKey.trim(), StandardCharsets.UTF_8);

        StringBuilder uri = new StringBuilder(baseUrl.trim())
                .append("?serviceKey=").append(encodedServiceKey)
                .append("&pageNo=").append(pageNo)
                .append("&numOfRows=").append(numOfRows)
                .append("&type=json");

        if (hasText(ctprvnNm)) {
            uri.append("&ctprvnNm=")
                    .append(URLEncoder.encode(ctprvnNm.trim(), StandardCharsets.UTF_8));
        }

        if (hasText(signguNm)) {
            uri.append("&signguNm=")
                    .append(URLEncoder.encode(signguNm.trim(), StandardCharsets.UTF_8));
        }

        return uri.toString();
    }

    private boolean upsertCrosswalk(JsonNode item) {
        String crosswalkCode = text(item, "crslkManageNo");
        if (!hasText(crosswalkCode)) {
            log.warn("[NATIONAL CROSSWALK SKIP] missing crslkManageNo. item={}", item);
            return false;
        }

        Double latitude = parseDouble(item, "latitude");
        Double longitude = parseDouble(item, "longitude");

        if (latitude == null || longitude == null) {
            log.warn("[NATIONAL CROSSWALK SKIP] invalid coordinates. code={}, latitude={}, longitude={}",
                    crosswalkCode, text(item, "latitude"), text(item, "longitude"));
            return false;
        }

        String sido = text(item, "ctprvnNm");
        String sigungu = text(item, "signguNm");
        String roadAddress = text(item, "rdnmadr");

        String kind = text(item, "crslkKnd");
        Double width = parseDouble(item, "bt");
        Double length = parseDouble(item, "et");

        Boolean pedestrianSignal = parseYn(item, "tfclghtYn");
        Boolean actuatedSignal = parseYn(item, "fnctngSgngnrYn");
        Integer greenTime = parseInteger(item, "greenSgngnrTime");
        Integer redTime = parseInteger(item, "redSgngnrTime");
        Boolean brailleBlock = parseYn(item, "brllBlckYn");
        Boolean curbLowered = parseYn(item, "ftpthLowerYn");
        Boolean trafficIsland = parseYn(item, "tfcilndYn");
        Boolean safetyLighting = parseYn(item, "cnctrLghtFcltyYn");

        LocalDate referenceDate = parseLocalDate(item, "referenceDate");
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        LocalDateTime lastSyncedAt = LocalDateTime.now();

        Optional<Crosswalk> optional = crosswalkRepository.findByCrosswalkCode(crosswalkCode);

        if (optional.isPresent()) {
            optional.get().updateFrom(
                    latitude,
                    longitude,
                    roadAddress,
                    sido,
                    sigungu,
                    null,
                    kind,
                    width,
                    length,
                    pedestrianSignal,
                    actuatedSignal,
                    greenTime,
                    redTime,
                    brailleBlock,
                    curbLowered,
                    trafficIsland,
                    safetyLighting,
                    DataSourceType.NATIONAL_STANDARD_CROSSWALK,
                    referenceDate,
                    lastSyncedAt
            );
        } else {
            crosswalkRepository.save(
                    Crosswalk.builder()
                            .crosswalkCode(crosswalkCode)
                            .latitude(latitude)
                            .longitude(longitude)
                            .roadAddress(roadAddress)
                            .sido(sido)
                            .sigungu(sigungu)
                            .emd(null)
                            .kind(kind)
                            .width(width)
                            .length(length)
                            .pedestrianSignal(pedestrianSignal)
                            .actuatedSignal(actuatedSignal)
                            .greenTime(greenTime)
                            .redTime(redTime)
                            .brailleBlock(brailleBlock)
                            .curbLowered(curbLowered)
                            .trafficIsland(trafficIsland)
                            .safetyLighting(safetyLighting)
                            .baseSource(DataSourceType.NATIONAL_STANDARD_CROSSWALK)
                            .referenceDate(referenceDate)
                            .lastSyncedAt(lastSyncedAt)
                            .build()
            );
        }

        return true;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return hasText(text) ? text.trim() : null;
    }

    private Double parseDouble(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("[NATIONAL CROSSWALK PARSE DOUBLE FAIL] field={}, value={}", fieldName, value);
            return null;
        }
    }

    private Integer parseInteger(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (!hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[NATIONAL CROSSWALK PARSE INTEGER FAIL] field={}, value={}", fieldName, value);
            return null;
        }
    }

    private Boolean parseYn(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (!hasText(value)) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "Y", "YES", "TRUE", "1" -> true;
            case "N", "NO", "FALSE", "0" -> false;
            default -> null;
        };
    }

    private LocalDate parseLocalDate(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (!hasText(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("[NATIONAL CROSSWALK PARSE DATE FAIL] field={}, value={}", fieldName, value);
            return null;
        }
    }

    private String maskUri(String uri) {
        if (uri == null || appKey == null) {
            return uri;
        }

        return uri.replace(appKey, "****")
                .replace(URLEncoder.encode(appKey, StandardCharsets.UTF_8), "****");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}