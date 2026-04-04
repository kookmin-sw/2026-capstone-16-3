package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CrosswalkRegionResolverService {

    @Value("${kakao.local.base-url}")
    private String kakaoLocalBaseUrl;

    @Value("${kakao.local.rest-api-key}")
    private String kakaoRestApiKey;

    private final WebClient webClient;

    public RegionInfo resolve(double latitude, double longitude) {
        Map<String, Object> response;
        try {
            response = webClient.get()
                    .uri(kakaoLocalBaseUrl + "/v2/local/geo/coord2regioncode.json?x=" + longitude + "&y=" + latitude)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                    "현재 위치를 행정구역으로 변환하지 못했습니다."
            );
        }

        if (response == null || !(response.get("documents") instanceof List<?> documents) || documents.isEmpty()) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_NO_DATA.code(),
                    "현재 위치에 대한 행정구역 정보를 찾지 못했습니다."
            );
        }

        Object first = documents.getFirst();
        if (!(first instanceof Map<?, ?> document)) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                    "행정구역 응답 형식이 올바르지 않습니다."
            );
        }

        String region1DepthName = stringValue(document.get("region_1depth_name"));
        String region2DepthName = stringValue(document.get("region_2depth_name"));

        if (region1DepthName == null || region2DepthName == null) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                    "행정구역 필수 값이 누락되었습니다."
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

    public record RegionInfo(String ctprvnNm, String signguNm) {
    }
}
