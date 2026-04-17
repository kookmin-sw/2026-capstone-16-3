package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
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

            throw new BusinessException(errorCode);
        } catch (WebClientRequestException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                throw new BusinessException(CrosswalkErrorCode.REGION_RESOLVE_TIMEOUT);
            }

            throw new BusinessException(CrosswalkErrorCode.REGION_RESOLVE_FAILED);
        } catch (Exception e) {
            throw new BusinessException(CrosswalkErrorCode.REGION_RESOLVE_FAILED);
        }

        if (response == null || !(response.get("documents") instanceof List<?> documents) || documents.isEmpty()) {
            throw new BusinessException(CrosswalkErrorCode.REGION_NOT_FOUND);
        }

        Object first = documents.getFirst();
        if (!(first instanceof Map<?, ?> document)) {
            throw new BusinessException(CrosswalkErrorCode.INVALID_REGION_RESPONSE);
        }

        String region1DepthName = stringValue(document.get("region_1depth_name"));
        String region2DepthName = stringValue(document.get("region_2depth_name"));

        if (region1DepthName == null || region2DepthName == null) {
            throw new BusinessException(CrosswalkErrorCode.INVALID_REGION_RESPONSE);
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

    public record RegionInfo(String ctprvnNm, String signguNm) {
    }
}