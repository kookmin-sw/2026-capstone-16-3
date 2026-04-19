package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Slf4j
@Component
public class KakaoLocalClient {

    private static final int MAX_PAGE = 45;
    private static final int MAX_RADIUS = 20_000;
    private static final int MAX_SIZE = 15;
    private static final Duration KAKAO_TIMEOUT = Duration.ofSeconds(5);
    private static final String SORT_DISTANCE = "distance";
    private static final String SORT_ACCURACY = "accuracy";

    private final WebClient webClient;

    public KakaoLocalClient(@Qualifier("kakaoLocalWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<KakaoCategorySearchResponse> searchCategory(
            String categoryCode,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        return request(
                uriBuilder -> uriBuilder.path("/v2/local/search/category.json")
                        .queryParam("category_group_code", categoryCode)
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("radius", clamp(radiusM, 0, MAX_RADIUS))
                        .queryParam("page", clamp(page, 1, MAX_PAGE))
                        .queryParam("size", clamp(size, 1, MAX_SIZE))
                        .queryParam("sort", SORT_DISTANCE)
                        .build(),
                KakaoCategorySearchResponse.class,
                "KAKAO CATEGORY"
        );
    }

    public Mono<KakaoCategorySearchResponse> searchKeyword(
            String query,
            double lat,
            double lng,
            int radiusM,
            int size,
            int page
    ) {
        return request(
                uriBuilder -> uriBuilder.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("radius", clamp(radiusM, 0, MAX_RADIUS))
                        .queryParam("page", clamp(page, 1, MAX_PAGE))
                        .queryParam("size", clamp(size, 1, MAX_SIZE))
                        .queryParam("sort", SORT_ACCURACY)
                        .build(),
                KakaoCategorySearchResponse.class,
                "KAKAO KEYWORD"
        );
    }

    public Mono<KakaoAddressSearchResponse> searchAddress(String query) {
        return request(
                uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                        .queryParam("query", query)
                        .queryParam("analyze_type", "similar")
                        .build(),
                KakaoAddressSearchResponse.class,
                "KAKAO ADDRESS"
        );
    }

    public Mono<KakaoCoordToAddressResponse> coordToAddress(double lat, double lng) {
        return request(
                uriBuilder -> uriBuilder.path("/v2/local/geo/coord2address.json")
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("input_coord", "WGS84")
                        .build(),
                KakaoCoordToAddressResponse.class,
                "KAKAO COORD2ADDR"
        );
    }

    private <T> Mono<T> request(
            Function<UriBuilder, URI> uriFunction,
            Class<T> responseType,
            String logPrefix
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    URI uri = uriFunction.apply(uriBuilder);
                    log.info("[{} REQ] uri={}", logPrefix, uri);
                    return uri;
                })
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[{} 4XX] status={}, body={}",
                                            logPrefix, response.statusCode(), body);
                                    return Mono.error(new PlaceException(
                                            PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX
                                    ));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[{} 5XX] status={}, body={}",
                                            logPrefix, response.statusCode(), body);
                                    return Mono.error(new PlaceException(
                                            PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX
                                    ));
                                })
                )
                .bodyToMono(responseType)
                .timeout(KAKAO_TIMEOUT)
                .onErrorMap(TimeoutException.class, e -> {
                    log.error("[{} TIMEOUT]", logPrefix, e);
                    return new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT);
                })
                .onErrorMap(WebClientRequestException.class, e -> {
                    log.error("[{} CONNECTION ERROR]", logPrefix, e);
                    return new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR);
                })
                .onErrorMap(
                        e -> !(e instanceof PlaceException),
                        e -> {
                            log.error("[{} UNKNOWN ERROR]", logPrefix, e);
                            return new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
                        }
                );
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}