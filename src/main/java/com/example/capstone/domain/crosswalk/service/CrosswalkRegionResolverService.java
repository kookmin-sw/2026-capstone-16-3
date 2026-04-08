package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.global.api.ErrorDetail;
import com.example.capstone.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class CrosswalkRegionResolverService {

    @Value("${kakao.local.base-url}")
    private String kakaoLocalBaseUrl;

    @Value("${kakao.local.rest-api-key}")
    private String kakaoRestApiKey;

    private final WebClient webClient;

    public CrosswalkRegionResolverService(
            @Qualifier("kakaoLocalWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }

    public RegionInfo resolve(double latitude, double longitude) {
        String uri = kakaoLocalBaseUrl + "/v2/local/geo/coord2regioncode.json?x=" + longitude + "&y=" + latitude;

        Map<String, Object> response;
        try {
            response = webClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            CrosswalkErrorCode errorCode = e.getStatusCode().is4xxClientError()
                    ? CrosswalkErrorCode.REGION_RESOLVE_HTTP_4XX
                    : CrosswalkErrorCode.REGION_RESOLVE_HTTP_5XX;

            throw new BusinessException(
                    errorCode.code(),
                    errorCode.message(),
                    errorCode.status(),
                    List.of(
                            ErrorDetail.of("uri", uri),
                            ErrorDetail.of("httpStatus", e.getStatusCode()),
                            ErrorDetail.of("responseBody", shorten(e.getResponseBodyAsString()))
                    )
            );
        } catch (WebClientRequestException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                throw new BusinessException(
                        CrosswalkErrorCode.REGION_RESOLVE_TIMEOUT.code(),
                        CrosswalkErrorCode.REGION_RESOLVE_TIMEOUT.message(),
                        CrosswalkErrorCode.REGION_RESOLVE_TIMEOUT.status(),
                        List.of(
                                ErrorDetail.of("uri", uri),
                                ErrorDetail.of("cause", cause.getClass().getSimpleName())
                        )
                );
            }

            throw new BusinessException(
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.code(),
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.message(),
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.status(),
                    List.of(
                            ErrorDetail.of("uri", uri),
                            ErrorDetail.of("cause", cause != null ? cause.getClass().getSimpleName() : e.getClass().getSimpleName()),
                            ErrorDetail.of("message", e.getMessage())
                    )
            );
        } catch (Exception e) {
            throw new BusinessException(
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.code(),
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.message(),
                    CrosswalkErrorCode.REGION_RESOLVE_FAILED.status(),
                    List.of(
                            ErrorDetail.of("uri", uri),
                            ErrorDetail.of("cause", e.getClass().getSimpleName()),
                            ErrorDetail.of("message", e.getMessage())
                    )
            );
        }

        if (response == null || !(response.get("documents") instanceof List<?> documents) || documents.isEmpty()) {
            throw new BusinessException(
                    CrosswalkErrorCode.REGION_NOT_FOUND.code(),
                    CrosswalkErrorCode.REGION_NOT_FOUND.message(),
                    CrosswalkErrorCode.REGION_NOT_FOUND.status(),
                    List.of(
                            ErrorDetail.of("uri", uri),
                            ErrorDetail.of("documents", response != null ? response.get("documents") : null)
                    )
            );
        }

        Object first = documents.getFirst();
        if (!(first instanceof Map<?, ?> document)) {
            throw new BusinessException(
                    CrosswalkErrorCode.INVALID_REGION_RESPONSE.code(),
                    CrosswalkErrorCode.INVALID_REGION_RESPONSE.message(),
                    CrosswalkErrorCode.INVALID_REGION_RESPONSE.status(),
                    List.of(
                            ErrorDetail.of("uri", uri),
                            ErrorDetail.of("firstDocumentType", first != null ? first.getClass().getName() : null)
                    )
            );
        }

        String region1DepthName = stringValue(document.get("region_1depth_name"));
        String region2DepthName = stringValue(document.get("region_2depth_name"));

        if (region1DepthName == null || region2DepthName == null) {
            throw new BusinessException(
                    CrosswalkErrorCode.INVALID_REGION_RESPONSE.code(),
                    "행정구역 필수 값이 누락되었습니다.",
                    CrosswalkErrorCode.INVALID_REGION_RESPONSE.status(),
                    List.of(
                            ErrorDetail.of("region_1depth_name", region1DepthName),
                            ErrorDetail.of("region_2depth_name", region2DepthName)
                    )
            );
        }

        return new RegionInfo(normalizeCtprvn(region1DepthName), normalizeSigngu(region2DepthName));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeCtprvn(String value) {
        return value.trim();
    }

    private String normalizeSigngu(String value) {
        return value.trim();
    }

    private String shorten(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 1000 ? value.substring(0, 1000) + "...(truncated)" : value;
    }

    public record RegionInfo(String ctprvnNm, String signguNm) {
    }
}