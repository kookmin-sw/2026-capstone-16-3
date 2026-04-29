package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.*;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToRegionCodeResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;
    private final PlaceAddressResolver placeAddressResolver;
    private final PlaceCache placeCache;

    private static final int MAX_PAGE = 45;
    private static final int MAX_SIZE = 15;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceCache placeCache, PlaceAddressResolver placeAddressResolver) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.placeAddressResolver = placeAddressResolver;
        this.placeCache = placeCache;
    }

    public SliceResponse<PlaceResponse> searchNearbyByCategory(
            String categoryCode,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        validateQuery(categoryCode);
        String normalizedCode = categoryCode.trim().toUpperCase();
        if (normalizedCode.contains(",")) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        validateLatLng(lat, lng);

        int kakaoPage = Math.min(Math.max(page, 1), MAX_PAGE);
        int kakaoSize = Math.min(Math.max(size, 1), MAX_SIZE); // Kakao limit

        try {
            KakaoCategorySearchResponse resp = kakaoLocalClient.searchCategory(
                            normalizedCode, lat, lng, radiusM, kakaoSize, kakaoPage
                    )
                    .doOnSubscribe(s -> log.info("[category search] start categoryCode='{}', page={}, size={}, lat={}, lng={}, radius={}",
                            categoryCode, kakaoPage, kakaoSize, lat, lng, radiusM))
                    .doOnError(e -> log.error("[category search] fail categoryCode='{}'", categoryCode, e))
                    .block();

            if (resp == null) {
                return new SliceResponse<>(List.of(), kakaoPage, kakaoSize, false, null);
            }

            boolean hasNext = resp.meta() != null && !resp.meta().isEnd();

            List<PlaceResponse> items =
                    (resp.documents() == null ? List.<KakaoCategorySearchResponse.KakaoPlaceDocument>of() : resp.documents())
                            .stream()
                            .map(doc -> toSearchPlace(doc, lat, lng))
                            .toList();

            return SliceResponse.of(items, page, kakaoSize, hasNext);
        } catch (PlaceException e) {
            throw e;
        } catch (Exception e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
    }

    public SliceResponse<PlaceResponse> searchPlaces(
            String query,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        validateQuery(query);
        validateLatLng(lat, lng);

        int kakaoPage = Math.min(Math.max(page, 1), MAX_PAGE);
        int kakaoSize = Math.min(Math.max(size, 1), MAX_SIZE); // Kakao limit

        try {
            KakaoCategorySearchResponse resp = kakaoLocalClient.searchKeyword(
                            query.trim(), lat, lng, radiusM, kakaoSize, kakaoPage
                    )
                    .doOnSubscribe(s -> log.info("[place search] start query='{}', page={}, size={}, lat={}, lng={}, radius={}",
                            query, kakaoPage, kakaoSize, lat, lng, radiusM))
                    .doOnError(e -> log.error("[place search] fail query='{}'", query, e))
                    .block();

            if (resp == null) {
                return new SliceResponse<>(List.of(), page, kakaoSize, false, null);
            }

            boolean hasNext = resp.meta() != null && !resp.meta().isEnd();

            List<PlaceResponse> items =
                    (resp.documents() == null ? List.<KakaoCategorySearchResponse.KakaoPlaceDocument>of() : resp.documents())
                            .stream()
                            .map(doc -> toSearchPlace(doc, lat, lng))
                            .toList();
            return SliceResponse.of(items, page, kakaoSize, hasNext);
        } catch (WebClientResponseException e) {
            throw new PlaceException(
                    e.getStatusCode().is4xxClientError()
                    ? PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX
                    : PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX
            );
        } catch (WebClientRequestException e) {
            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR
            );
        } catch (PlaceException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();

            if (cause instanceof TimeoutException) {
                throw new PlaceException(
                        PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT
                );
            }

            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
            );
        }
    }

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument doc = placeCache.get(placeId)
                .orElseThrow(() -> new PlaceException(
                        PlaceErrorCode.PLACE_CACHE_MISS
                ));

        double placeLng = Double.parseDouble(doc.x());
        double placeLat = Double.parseDouble(doc.y());
        Long dist = (doc.distance() == null || doc.distance().isBlank()) ? null : Long.parseLong(doc.distance());

        String resolvedAddress = placeAddressResolver.normalizeAddress(doc.addressName());

        String resolvedRoadAddress = placeAddressResolver.resolveRoadAddress(
                doc.addressName(),
                doc.roadAddressName(),
                placeLat,
                placeLng
        );

        return new PlaceDetailResponse(
                placeId,
                doc.placeName(),
                doc.categoryName(),
                resolvedAddress,
                resolvedRoadAddress,
                placeLat,
                placeLng,
                dist,
                doc.phone(),
                doc.placeUrl(),
                null,
                new PlaceDetailResponse.Extra("KAKAO")
        );
    }

    private PlaceResponse toSearchPlace(
            KakaoCategorySearchResponse.KakaoPlaceDocument d,
            double originLat,
            double originLng
    ) {
        String placeId = toExternalPlaceId("KAKAO", d.id());
        placeCache.put(placeId, d);

        double placeLng = Double.parseDouble(d.x());
        double placeLat = Double.parseDouble(d.y());
        Long dist = (d.distance() == null || d.distance().isBlank()) ? null : Long.parseLong(d.distance());
        if (dist == null) {
            dist = haversineMeters(originLat, originLng, placeLat, placeLng);
        }

        Integer directionClock = toDirectionClock(originLat, originLng, placeLat, placeLng);

        String resolvedRoadAddress = resolveListRoadAddress(
                d.addressName(),
                d.roadAddressName()
        );

        return new PlaceResponse(
                placeId,
                d.placeName(),
                d.categoryName(),
                resolvedRoadAddress,
                placeLat,
                placeLng,
                dist,
                directionClock
        );
    }

    public GeocodeResponse geocode(String query) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        KakaoAddressSearchResponse resp = kakaoLocalClient.searchAddress(query.trim())
                .doOnSubscribe(s -> log.info("Kakao geocode start query='{}'", query))
                .doOnError(e -> log.error("Kakao geocode fail query='{}'", query, e))
                .block();

        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
            throw new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        KakaoAddressSearchResponse.Document doc = resp.documents().getFirst();

        try {
            double lng = Double.parseDouble(doc.x());
            double lat = Double.parseDouble(doc.y());

            String address = placeAddressResolver.normalizeAddress(doc.addressName());
            String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;

            String resolvedRoadAddress = placeAddressResolver.resolveRoadAddress(
                    doc.addressName(),
                    roadAddress,
                    lat,
                    lng
            );

            return new GeocodeResponse(
                    query.trim(),
                    address,
                    resolvedRoadAddress,
                    lat,
                    lng
            );
        } catch (Exception e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
    }

    public ReverseGeocodeResponse reverseGeocode(double lat, double lng) {
        validateLatLng(lat, lng);

        try {
            KakaoCoordToAddressResponse addressResp = kakaoLocalClient.coordToAddress(lat, lng)
                    .doOnSubscribe(s -> log.info("Kakao reverse geocode start lat={}, lng={}", lat, lng))
                    .doOnError(e -> log.error("Kakao reverse geocode fail lat={}, lng={}", lat, lng, e))
                    .block();

            if (addressResp != null && addressResp.documents() != null && !addressResp.documents().isEmpty()) {
                KakaoCoordToAddressResponse.Document doc = addressResp.documents().getFirst();

                String address = doc.address() != null ? doc.address().addressName() : null;
                String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;

                String normalizedAddress = placeAddressResolver.normalizeAddress(address);
                String resolvedRoadAddress = placeAddressResolver.resolveRoadAddress(address, roadAddress, lat, lng);

                if (hasText(resolvedRoadAddress) || hasText(normalizedAddress)) {
                    return ReverseGeocodeResponse.ofAddress(
                            normalizedAddress,
                            resolvedRoadAddress,
                            lat,
                            lng
                    );
                }
            }

            KakaoCoordToRegionCodeResponse regionResp = kakaoLocalClient.coordToRegionCode(lat, lng)
                    .doOnSubscribe(s -> log.info("Kakao coord2region start lat={}, lng={}", lat, lng))
                    .doOnError(e -> log.error("Kakao coord2region fail lat={}, lng={}", lat, lng, e))
                    .block();

            KakaoCoordToRegionCodeResponse.Document regionDoc = selectRegionDocument(regionResp);
            String regionAddress = regionDoc != null
                    ? placeAddressResolver.normalizeAddress(regionDoc.addressName())
                    : null;

            if (hasText(regionAddress)) {
                return ReverseGeocodeResponse.ofRegion(regionAddress, lat, lng);
            }

            throw new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
        } catch (PlaceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX);
            }
            if (e.getStatusCode().is5xxServerError()) {
                throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX);
            }
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        } catch (WebClientRequestException e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR);
        } catch (Exception e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
    }

    private KakaoCoordToRegionCodeResponse.Document selectRegionDocument(
            KakaoCoordToRegionCodeResponse regionResp
    ) {
        if (regionResp == null || regionResp.documents() == null || regionResp.documents().isEmpty()) {
            return null;
        }

        return regionResp.documents().stream()
                .filter(doc -> "H".equals(doc.regionType()))
                .findFirst()
                .or(() -> regionResp.documents().stream()
                        .filter(doc -> "B".equals(doc.regionType()))
                        .findFirst())
                .orElse(regionResp.documents().getFirst());
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }
    }

    private void validateLatLng(double lat, double lng) {
        // 기본 좌표 범위 검증
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }

        // 한국 대략 범위 추가 검증
        if (lat < 33.0 || lat > 39.5 || lng < 124.0 || lng > 132.0) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
    }

    private static String toExternalPlaceId(String provider, String externalId) {
        return "ext:" + provider + ":" + externalId;
    }

    private static Integer toDirectionClock(double originLat, double originLng, double targetLat, double targetLng) {
        double bearing = bearingDegrees(originLat, originLng, targetLat, targetLng);

        // 0도=북(12시), 90도=동(3시) ... 30도 단위로 반올림
        int clock = (int) Math.floor((bearing + 15.0) / 30.0) % 12;
        return clock == 0 ? 12 : clock;
    }

    private static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);

        double theta = Math.atan2(y, x);
        double deg = Math.toDegrees(theta);

        // 0~360 정규화
        return (deg + 360.0) % 360.0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusMeters = 6_371_000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadiusMeters * c);
    }

    private String resolveListRoadAddress(
            String jibunAddress,
            String roadAddress
    ) {
        if (hasText(roadAddress)) {
            return placeAddressResolver.normalizeAddress(roadAddress);
        }

        return placeAddressResolver.normalizeAddress(jibunAddress);
    }
}