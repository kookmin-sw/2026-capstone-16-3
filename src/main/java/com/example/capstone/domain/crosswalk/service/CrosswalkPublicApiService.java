package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiItem;
import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiResponse;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.global.api.ErrorDetail;
import com.example.capstone.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class CrosswalkPublicApiService {

    @Value("${public-data.crosswalk.base-url}")
    private String baseUrl;

    @Value("${public-data.crosswalk.service-key}")
    private String serviceKey;

    @Value("${public-data.crosswalk.default-page-size:100}")
    private int pageSize;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CrosswalkPublicApiService(
            @Qualifier("defaultWebClient") WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode fetchRawJson(String ctprvnNm, String signguNm) {
        String uri = buildUri(ctprvnNm, signguNm, 10);
        return requestRawBody(uri, ctprvnNm, signguNm);
    }

    public List<CrosswalkApiItem> fetchByRegion(String ctprvnNm, String signguNm) {
        String uri = buildUri(ctprvnNm, signguNm, pageSize);
        JsonNode rawBody = requestRawBody(uri, ctprvnNm, signguNm);

        CrosswalkApiResponse response;
        try {
            response = objectMapper.treeToValue(rawBody, CrosswalkApiResponse.class);
        } catch (JsonProcessingException e) {
            log.error("횡단보도 공공데이터 응답 파싱 실패 - uri={}, rawBody={}", maskUri(uri), shorten(rawBody.toString()), e);

            throw business(
                    CrosswalkErrorCode.CROSSWALK_API_DECODING_ERROR,
                    ErrorDetail.of("uri", maskUri(uri)),
                    ErrorDetail.of("exception", e.getClass().getSimpleName()),
                    ErrorDetail.of("message", e.getOriginalMessage()),
                    ErrorDetail.of("rawBody", shorten(rawBody.toString()))
            );
        }

        if (response == null
                || response.getResponse() == null
                || response.getResponse().getHeader() == null) {
            log.error("횡단보도 공공데이터 응답 구조 이상 - uri={}, rawBody={}", maskUri(uri), shorten(rawBody.toString()));

            throw business(
                    CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE,
                    ErrorDetail.of("uri", maskUri(uri)),
                    ErrorDetail.of("rawBody", shorten(rawBody.toString()))
            );
        }

        String resultCode = response.getResponse().getHeader().getResultCode();
        String resultMsg = response.getResponse().getHeader().getResultMsg();

        log.info("횡단보도 공공데이터 헤더 - resultCode={}, resultMsg={}", resultCode, resultMsg);

        CrosswalkErrorCode openApiError = CrosswalkErrorCode.fromOpenApiCode(resultCode);
        if (openApiError != null) {
            if (openApiError == CrosswalkErrorCode.CROSSWALK_NO_DATA) {
                log.info("횡단보도 공공데이터 조회 결과 없음 - ctprvnNm={}, signguNm={}", ctprvnNm, signguNm);
                return Collections.emptyList();
            }

            throw business(
                    openApiError,
                    ErrorDetail.of("uri", maskUri(uri)),
                    ErrorDetail.of("resultCode", resultCode),
                    ErrorDetail.of("resultMsg", resultMsg),
                    ErrorDetail.of("rawBody", shorten(rawBody.toString()))
            );
        }

        if (response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null) {
            log.info("횡단보도 공공데이터 item 없음 - ctprvnNm={}, signguNm={}", ctprvnNm, signguNm);
            return Collections.emptyList();
        }

        List<CrosswalkApiItem> items = response.getResponse().getBody().getItems();
        log.info("first item latitude={}, longitude={}",
                items.isEmpty() ? null : items.getFirst().getLatitude(),
                items.isEmpty() ? null : items.getFirst().getLongitude());
        log.info("횡단보도 공공데이터 조회 완료 - itemCount={}", items.size());
        return items;
    }

    private JsonNode requestRawBody(String uri, String ctprvnNm, String signguNm) {
        log.info("횡단보도 공공데이터 요청 시작 - ctprvnNm={}, signguNm={}, uri={}",
                ctprvnNm, signguNm, maskUri(uri));

        String rawBody;
        try {
            rawBody = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error("횡단보도 공공데이터 4xx 응답 - status={}, body={}",
                                                clientResponse.statusCode(), shorten(body));

                                        return business(
                                                CrosswalkErrorCode.CROSSWALK_API_HTTP_4XX,
                                                ErrorDetail.of("uri", maskUri(uri)),
                                                ErrorDetail.of("httpStatus", clientResponse.statusCode().value()),
                                                ErrorDetail.of("rawBody", shorten(body))
                                        );
                                    }))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error("횡단보도 공공데이터 5xx 응답 - status={}, body={}",
                                                clientResponse.statusCode(), shorten(body));

                                        return business(
                                                CrosswalkErrorCode.CROSSWALK_API_HTTP_5XX,
                                                ErrorDetail.of("uri", maskUri(uri)),
                                                ErrorDetail.of("httpStatus", clientResponse.statusCode().value()),
                                                ErrorDetail.of("rawBody", shorten(body))
                                        );
                                    }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            Throwable root = Exceptions.unwrap(e);

            if (root instanceof TimeoutException) {
                log.error("횡단보도 공공데이터 타임아웃 - uri={}", maskUri(uri), e);
                throw business(
                        CrosswalkErrorCode.CROSSWALK_API_TIMEOUT,
                        ErrorDetail.of("uri", maskUri(uri)),
                        ErrorDetail.of("exception", e.getClass().getSimpleName()),
                        ErrorDetail.of("cause", root.getClass().getSimpleName()),
                        ErrorDetail.of("message", root.getMessage())
                );
            }

            log.error("횡단보도 공공데이터 호출 실패 - uri={}", maskUri(uri), e);
            throw business(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR,
                    ErrorDetail.of("uri", maskUri(uri)),
                    ErrorDetail.of("exception", e.getClass().getSimpleName()),
                    ErrorDetail.of("message", e.getMessage())
            );
        }

        log.info("횡단보도 공공데이터 raw body={}", shorten(rawBody));

        if (rawBody == null || rawBody.isBlank()) {
            throw business(
                    CrosswalkErrorCode.CROSSWALK_API_EMPTY_BODY,
                    ErrorDetail.of("uri", maskUri(uri))
            );
        }

        try {
            return objectMapper.readTree(rawBody);
        } catch (JsonProcessingException e) {
            log.error("횡단보도 공공데이터 raw JSON 파싱 실패 - uri={}, rawBody={}",
                    maskUri(uri), shorten(rawBody), e);

            throw business(
                    CrosswalkErrorCode.CROSSWALK_API_DECODING_ERROR,
                    ErrorDetail.of("uri", maskUri(uri)),
                    ErrorDetail.of("exception", e.getClass().getSimpleName()),
                    ErrorDetail.of("message", e.getOriginalMessage()),
                    ErrorDetail.of("rawBody", shorten(rawBody))
            );
        }
    }

    private String buildUri(String ctprvnNm, String signguNm, int numOfRows) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .queryParam("ctprvnNm", ctprvnNm)
                .queryParam("signguNm", signguNm)
                .build(false)
                .toUriString();
    }

    private BusinessException business(CrosswalkErrorCode errorCode, ErrorDetail... details) {
        return new BusinessException(
                errorCode.code(),
                errorCode.message(),
                errorCode.status(),
                List.of(details)
        );
    }

    private String maskUri(String uri) {
        return serviceKey == null ? uri : uri.replace(serviceKey, "****");
    }

    private String shorten(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 1000 ? value.substring(0, 1000) + "...(truncated)" : value;
    }
}