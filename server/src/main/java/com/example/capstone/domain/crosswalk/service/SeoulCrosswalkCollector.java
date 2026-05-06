package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import com.example.capstone.domain.crosswalk.service.sync.CrosswalkSyncStats;
import com.example.capstone.domain.crosswalk.service.sync.FailureReason;
import com.example.capstone.domain.crosswalk.service.sync.PublicDataCollector;
import com.example.capstone.global.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SeoulCrosswalkCollector implements PublicDataCollector {

    private static final String SERVICE_NAME = "tbTraficCrsng";
    private static final int PAGE_SIZE = 1000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final Pattern POINT_WKT_PATTERN =
            Pattern.compile("POINT\\s*\\(([-\\d.]+)\\s+([-\\d.]+)\\)");

    private static final Pattern LINESTRING_WKT_PATTERN =
            Pattern.compile("LINESTRING\\s*\\((.+)\\)");

    private final WebClient webClient;
    private final CrosswalkRepository crosswalkRepository;

    @Value("${opendata.app-key}")
    private String seoulOpenDataAppKey;

    @Value("${opendata.base-url:http://openapi.seoul.go.kr:8088}")
    private String seoulOpenDataBaseUrl;

    public SeoulCrosswalkCollector(
            @Qualifier("opendataWebClient") WebClient webClient,
            CrosswalkRepository crosswalkRepository
    ) {
        this.webClient = webClient;
        this.crosswalkRepository = crosswalkRepository;
    }

    @Override
    public CrosswalkSyncStats collectAndSave() {
        CrosswalkSyncStats stats = new CrosswalkSyncStats("서울 횡단보도");

        int startIndex = 1;
        int endIndex = PAGE_SIZE;
        int totalCount = Integer.MAX_VALUE;

        while (startIndex <= totalCount) {
            JsonNode serviceNode = requestServiceNode(startIndex, endIndex);

            String resultCode = serviceNode.path("RESULT").path("CODE").asText();
            String resultMessage = serviceNode.path("RESULT").path("MESSAGE").asText();

            if ("INFO-200".equals(resultCode)) {
                log.debug("[SEOUL CROSSWALK COLLECT] no more data. startIndex={}, endIndex={}", startIndex, endIndex);
                break;
            }

            if (!"INFO-000".equals(resultCode)) {
                stats.increaseFailure(FailureReason.OPEN_API_ERROR);
                log.warn("[SEOUL CROSSWALK COLLECT] open api error. code={}, message={}", resultCode, resultMessage);
                throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
            }

            totalCount = serviceNode.path("list_total_count").asInt(0);
            JsonNode rows = serviceNode.path("row");

            if (!rows.isArray() || rows.isEmpty()) {
                log.debug("[SEOUL CROSSWALK COLLECT] empty rows. startIndex={}, endIndex={}", startIndex, endIndex);
                break;
            }

            int savedCount = 0;

            for (JsonNode row : rows) {
                stats.increaseTotal();

                if (upsertCrosswalk(row, stats)) {
                    stats.increaseSuccess();
                    savedCount++;
                }
            }

            log.debug(
                    "[SEOUL CROSSWALK COLLECT PAGE] range={}~{}, saved={}, totalCount={}",
                    startIndex, endIndex, savedCount, totalCount
            );

            startIndex += PAGE_SIZE;
            endIndex = startIndex + PAGE_SIZE - 1;
        }

        log.info(
                "[SEOUL CROSSWALK COLLECT SUMMARY] total={}, success={}, failure={}, failureReasons={}",
                stats.totalCount(),
                stats.successCount(),
                stats.failureCount(),
                stats.failureSummary()
        );

        return stats;
    }

    private JsonNode requestServiceNode(int startIndex, int endIndex) {
        String uri = String.format(
                "%s/%s/json/%s/%d/%d",
                seoulOpenDataBaseUrl,
                seoulOpenDataAppKey,
                SERVICE_NAME,
                startIndex,
                endIndex
        );

        JsonNode root = webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[SEOUL CROSSWALK 4XX] uri={}, status={}, body={}",
                                            uri, response.statusCode(), body);
                                    return Mono.error(new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_4XX));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[SEOUL CROSSWALK 5XX] uri={}, status={}, body={}",
                                            uri, response.statusCode(), body);
                                    return Mono.error(new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_5XX));
                                })
                )
                .bodyToMono(JsonNode.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(TimeoutException.class, e -> {
                    log.error("[SEOUL CROSSWALK TIMEOUT] uri={}", uri, e);
                    return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_TIMEOUT);
                })
                .onErrorMap(WebClientRequestException.class, e -> {
                    log.error("[SEOUL CROSSWALK CONNECTION ERROR] uri={}", uri, e);
                    return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR);
                })
                .onErrorMap(
                        e -> !(e instanceof BusinessException),
                        e -> {
                            log.error("[SEOUL CROSSWALK UNKNOWN ERROR] uri={}", uri, e);
                            return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
                        }
                )
                .block();

        if (root == null || root.path(SERVICE_NAME).isMissingNode()) {
            log.error("[SEOUL CROSSWALK INVALID RESPONSE] uri={}, root={}", uri, root);
            throw new BusinessException(CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE);
        }

        return root.path(SERVICE_NAME);
    }

    private boolean upsertCrosswalk(JsonNode row, CrosswalkSyncStats stats) {
        String type = firstNonBlank(
                text(row, "NODE_LINK_TYPE"),
                text(row, "노드링크 유형")
        );

        String crosswalkCode = buildCrosswalkCode(row, type);
        if (crosswalkCode == null) {
            stats.increaseFailure(FailureReason.MISSING_CODE);
            log.debug("[SEOUL CROSSWALK SKIP] missing code. row={}", row);
            return false;
        }

        Wgs84Point point = extractPoint(row, type);
        if (point == null) {
            stats.increaseFailure(FailureReason.INVALID_GEOMETRY);
            log.debug("[SEOUL CROSSWALK SKIP] invalid geometry. code={}, type={}, nodeWkt={}, linkWkt={}",
                    crosswalkCode, type,
                    firstNonBlank(text(row, "NODE_WKT"), text(row, "노드 WKT")),
                    firstNonBlank(text(row, "LINK_WKT"), text(row, "링크 WKT")));
            return false;
        }

        String kind = firstNonBlank(
                text(row, "LNKG_TYPE_CD"),
                text(row, "NODE_TYPE_CD"),
                text(row, "노드 유형 코드"),
                text(row, "링크 유형 코드"),
                text(row, "NODE_TYPE")
        );

        Double length = firstNonNullDouble(
                parseDouble(row, "LNKG_LEN"),
                parseDouble(row, "링크 길이")
        );

        String sigungu = firstNonBlank(
                text(row, "SGG_NM"),
                text(row, "시군구명")
        );

        String emd = firstNonBlank(
                text(row, "EMD_NM"),
                text(row, "읍면동명")
        );

        LocalDate referenceDate = LocalDate.now();
        LocalDateTime lastSyncedAt = LocalDateTime.now();

        Optional<Crosswalk> optional = crosswalkRepository.findByCrosswalkCode(crosswalkCode);

        if (optional.isPresent()) {
            optional.get().updateFrom(
                    point.latitude(),
                    point.longitude(),
                    null,
                    "서울특별시",
                    sigungu,
                    emd,
                    kind,
                    null,
                    length,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DataSourceType.SEOUL_TB_TRAFFIC_CRSNG,
                    referenceDate,
                    lastSyncedAt
            );
        } else {
            crosswalkRepository.save(
                    Crosswalk.builder()
                            .crosswalkCode(crosswalkCode)
                            .latitude(point.latitude())
                            .longitude(point.longitude())
                            .roadAddress(null)
                            .sido("서울특별시")
                            .sigungu(sigungu)
                            .emd(emd)
                            .kind(kind)
                            .width(null)
                            .length(length)
                            .pedestrianSignal(null)
                            .actuatedSignal(null)
                            .greenTime(null)
                            .redTime(null)
                            .brailleBlock(null)
                            .curbLowered(null)
                            .trafficIsland(null)
                            .safetyLighting(null)
                            .baseSource(DataSourceType.SEOUL_TB_TRAFFIC_CRSNG)
                            .referenceDate(referenceDate)
                            .lastSyncedAt(lastSyncedAt)
                            .build()
            );
        }

        return true;
    }

    private String buildCrosswalkCode(JsonNode row, String type) {
        String linkId = firstNonBlank(text(row, "LNKG_ID"), text(row, "링크 ID"));
        String nodeId = firstNonBlank(text(row, "NODE_ID"), text(row, "노드 ID"));

        if ("LINK".equalsIgnoreCase(type) && hasText(linkId) && !"0".equals(linkId) && !"0.0".equals(linkId)) {
            return "SEOUL-LINK-" + linkId;
        }

        if ("NODE".equalsIgnoreCase(type) && hasText(nodeId) && !"0".equals(nodeId) && !"0.0".equals(nodeId)) {
            return "SEOUL-NODE-" + nodeId;
        }

        if (hasText(linkId) && !"0".equals(linkId) && !"0.0".equals(linkId)) {
            return "SEOUL-LINK-" + linkId;
        }

        if (hasText(nodeId) && !"0".equals(nodeId) && !"0.0".equals(nodeId)) {
            return "SEOUL-NODE-" + nodeId;
        }

        return null;
    }

    private Wgs84Point extractPoint(JsonNode row, String type) {
        String nodeWkt = firstNonBlank(text(row, "NODE_WKT"), text(row, "노드 WKT"));
        String linkWkt = firstNonBlank(text(row, "LINK_WKT"), text(row, "링크 WKT"));

        if ("NODE".equalsIgnoreCase(type)) {
            return parsePointWkt(nodeWkt);
        }

        if ("LINK".equalsIgnoreCase(type)) {
            return parseLineStringMidPoint(linkWkt);
        }

        Wgs84Point point = parsePointWkt(nodeWkt);
        if (point != null) {
            return point;
        }
        return parseLineStringMidPoint(linkWkt);
    }

    private Wgs84Point parsePointWkt(String wkt) {
        if (!hasText(wkt)) {
            return null;
        }

        Matcher matcher = POINT_WKT_PATTERN.matcher(wkt.trim());
        if (!matcher.matches()) {
            return null;
        }

        double longitude = Double.parseDouble(matcher.group(1));
        double latitude = Double.parseDouble(matcher.group(2));
        return new Wgs84Point(latitude, longitude);
    }

    private Wgs84Point parseLineStringMidPoint(String wkt) {
        if (!hasText(wkt)) {
            return null;
        }

        Matcher matcher = LINESTRING_WKT_PATTERN.matcher(wkt.trim());
        if (!matcher.matches()) {
            return null;
        }

        String[] points = matcher.group(1).split(",");
        if (points.length == 0) {
            return null;
        }

        double sumLon = 0.0;
        double sumLat = 0.0;
        int count = 0;

        for (String point : points) {
            String[] parts = point.trim().split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            sumLon += Double.parseDouble(parts[0]);
            sumLat += Double.parseDouble(parts[1]);
            count++;
        }

        if (count == 0) {
            return null;
        }

        return new Wgs84Point(sumLat / count, sumLon / count);
    }

    private String text(JsonNode row, String fieldName) {
        JsonNode value = row.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return hasText(text) ? text.trim() : null;
    }

    private Double parseDouble(JsonNode row, String fieldName) {
        String value = text(row, fieldName);
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.debug("[SEOUL CROSSWALK PARSE DOUBLE FAIL] field={}, value={}", fieldName, value);
            return null;
        }
    }

    private Double firstNonNullDouble(Double... values) {
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Wgs84Point(double latitude, double longitude) {
    }
}