package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.naver.NaverAddressCandidate;
import com.example.capstone.domain.place.dto.response.naver.NaverGeocodeResponse;
import com.example.capstone.domain.place.dto.response.naver.NaverReverseGeocodeResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class NaverMapClient {

    private static final Duration NAVER_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String appKey;
    private final String appSecret;

    public NaverMapClient(
            @Qualifier("naverWebClient") WebClient naverWebClient,
            @Value("${naver.map.app-key}") String appKey,
            @Value("${naver.map.secret}") String appSecret
    ) {
        this.webClient = naverWebClient;
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public Mono<NaverGeocodeResponse> geocode(String query) {
        return request(
                uriBuilder -> uriBuilder.path("/map-geocode/v2/geocode")
                        .queryParam("query", query)
                        .build(),
                NaverGeocodeResponse.class,
                "NAVER GEOCODE"
        );
    }

    public Mono<NaverAddressCandidate> reverseGeocode(double lat, double lng) {
        return request(
                uriBuilder -> uriBuilder.path("/map-reversegeocode/v2/gc")
                        .queryParam("coords", lng + "," + lat)
                        .queryParam("sourcecrs", "epsg:4326")
                        .queryParam("output", "json")
                        .queryParam("orders", "legalcode,admcode,addr,roadaddr")
                        .build(),
                NaverReverseGeocodeResponse.class,
                "NAVER REVERSE GEOCODE"
        ).map(this::toAddressCandidate);
    }

    private NaverAddressCandidate toAddressCandidate(NaverReverseGeocodeResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return new NaverAddressCandidate(null, null);
        }

        String roadAddress = null;
        String jibunAddress = null;

        for (NaverReverseGeocodeResponse.Result result : response.results()) {
            if ("roadaddr".equals(result.name())) {
                roadAddress = buildNaverRoadAddress(result);
            }

            if ("addr".equals(result.name())) {
                jibunAddress = buildNaverJibunAddress(result);
            }
        }

        log.info(
                "[naver reverse geocode result] road='{}', jibun='{}'",
                roadAddress,
                jibunAddress
        );

        return new NaverAddressCandidate(roadAddress, jibunAddress);
    }

    private String buildNaverRoadAddress(NaverReverseGeocodeResponse.Result result) {
        if (result == null || result.region() == null || result.land() == null) {
            return null;
        }

        String area1 = areaName(result.region().area1());
        String area2 = areaName(result.region().area2());
        String roadName = result.land().name();
        String mainNo = result.land().number1();
        String subNo = result.land().number2();

        if (!hasText(area1) || !hasText(area2) || !hasText(roadName) || !hasText(mainNo)) {
            return null;
        }

        String buildingNo = hasText(subNo)
                ? mainNo + "-" + subNo
                : mainNo;

        return String.join(" ", area1, area2, roadName, buildingNo);
    }

    private String buildNaverJibunAddress(NaverReverseGeocodeResponse.Result result) {
        if (result == null || result.region() == null || result.land() == null) {
            return null;
        }

        String area1 = areaName(result.region().area1());
        String area2 = areaName(result.region().area2());
        String area3 = areaName(result.region().area3());
        String mainNo = result.land().number1();
        String subNo = result.land().number2();

        if (!hasText(area1) || !hasText(area2) || !hasText(area3) || !hasText(mainNo)) {
            return null;
        }

        String lotNo = hasText(subNo)
                ? mainNo + "-" + subNo
                : mainNo;

        return String.join(" ", area1, area2, area3, lotNo);
    }

    private String areaName(NaverReverseGeocodeResponse.Area area) {
        if (area == null || !hasText(area.name())) {
            return null;
        }

        return area.name();
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
                .header("x-ncp-apigw-api-key-id", appKey)
                .header("x-ncp-apigw-api-key", appSecret)
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
                .timeout(NAVER_TIMEOUT)
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}