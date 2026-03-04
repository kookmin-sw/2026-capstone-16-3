package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.util.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.net.URI;


@Slf4j
@Component
public class KakaoLocalClient {

    private static final int MAX_PAGE = 45;
    private final WebClient webClient;

    public KakaoLocalClient(
            @Value("${kakao.local.base-url:https://dapi.kakao.com}") String baseUrl,
            @Value("${kakao.local.rest-api-key}") String restApiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    log.info("[KAKAO REQ] {} {}", req.method(), req.url());
                    return Mono.just(req);
                }))
                .build();
    }

    public Mono<KakaoCategorySearchResponse> searchCategory(
            String categoryCode,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        return webClient.get()
                .uri(b -> buildCategoryUri(b, categoryCode, lat, lng, radiusM, size, page))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("[KAKAO RES] status={}, body={}", resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("Kakao API error: " + resp.statusCode()));
                                })
                )
                .bodyToMono(KakaoCategorySearchResponse.class);
    }

    public Mono<KakaoCategorySearchResponse> searchKeyword(
            String query,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        return webClient.get()
                .uri(b -> buildKeywordUri(b, query, lat, lng, radiusM, size, page))
                .retrieve()
                .bodyToMono(KakaoCategorySearchResponse.class);
    }

    private URI buildCategoryUri(
            UriBuilder b,
            String code,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        int radius = Math.min(Math.max(radiusM, 0), 3000);
        int safeSize = Math.min(Math.max(size, 1), 15);
        int safePage = Math.min(Math.max(page, 1), 45);

        URI uri = b.path("/v2/local/search/category.json")
                .queryParam("category_group_code", code)
                .queryParam("x", lng) // x=경도
                .queryParam("y", lat) // y=위도
                .queryParam("radius", Math.min(Math.max(radiusM, 0), 3000))
                .queryParam("sort", "distance")
                .queryParam("size", Math.min(Math.max(size, 1), 15))
                .queryParam("page", Math.min(Math.max(page, 1), 45))
                .build();

        log.info("[KAKAO REQ] uri={}", uri);
        return uri;
    }

    private URI buildKeywordUri(
            UriBuilder b,
            String query,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        return b.path("/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", clamp(radiusM, 0, 3000))
                .queryParam("sort", "distance")
                .queryParam("size", clamp(size, 1, 15))
                .queryParam("page", clamp(page, 1, 45))
                .build();
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(Math.max(v, min), max);
    }
}