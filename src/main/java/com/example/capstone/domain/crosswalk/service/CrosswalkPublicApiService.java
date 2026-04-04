package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiItem;
import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiResponse;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.global.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrosswalkPublicApiService {

    @Value("${public-data.crosswalk.base-url}")
    private String baseUrl;

    @Value("${public-data.crosswalk.service-key}")
    private String serviceKey;

    @Value("${public-data.crosswalk.default-page-size:100}")
    private int pageSize;

    private final WebClient webClient;

    public List<CrosswalkApiItem> fetchByRegion(String ctprvnNm, String signguNm) {
        String uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", pageSize)
                .queryParam("type", "json")
                .queryParam("ctprvnNm", ctprvnNm)
                .queryParam("signguNm", signguNm)
                .build(false)
                .toUriString();

        CrosswalkApiResponse response;
        try {
            response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> new BusinessException(
                                            CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                                            CrosswalkErrorCode.CROSSWALK_API_ERROR.message()
                                    )))
                    .bodyToMono(CrosswalkApiResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.message()
            );
        }

        if (response == null || response.getResponse() == null || response.getResponse().getHeader() == null) {
            throw new BusinessException(
                    CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE.code(),
                    CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE.message()
            );
        }

        CrosswalkErrorCode openApiError = CrosswalkErrorCode.fromOpenApiCode(response.getResponse().getHeader().getResultCode());
        if (openApiError != null && openApiError != CrosswalkErrorCode.CROSSWALK_NO_DATA) {
            throw new BusinessException(openApiError.code(), openApiError.message());
        }

        if (response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }

        return response.getResponse().getBody().getItems().getItem();
    }

    public String fetchTest(String ctprvnNm, String signguNm) {
        String raw = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getCrosswalkInfoList")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 10)
                        .queryParam("type", "json")
                        .queryParam("ctprvnNm", ctprvnNm)
                        .queryParam("signguNm", signguNm)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

        return raw;
    }
}
