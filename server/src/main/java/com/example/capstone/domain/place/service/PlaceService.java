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
    private final PlaceCache placeCache;

    private static final int MAX_PAGE = 45;
    private static final int MAX_SIZE = 15;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceCache placeCache) {
        this.kakaoLocalClient = kakaoLocalClient;
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

        String resolvedAddress = normalizeAddress(doc.addressName());

        String resolvedRoadAddress = resolveRoadAddress(
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

        String resolvedRoadAddress = resolveRoadAddress(
                d.addressName(),
                d.roadAddressName(),
                placeLat,
                placeLng
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

            String address = normalizeAddress(doc.addressName());
            String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;

            String resolvedRoadAddress = resolveRoadAddress(
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

                String normalizedAddress = normalizeAddress(address);
                String resolvedRoadAddress = resolveRoadAddress(address, roadAddress, lat, lng);

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
                    ? normalizeAddress(regionDoc.addressName())
                    : null;

            ReverseGeocodeResponse nearestPlace = findNearestPlaceFallback(regionDoc, regionAddress, lat, lng);
            if (nearestPlace != null) {
                return nearestPlace;
            }

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

    private ReverseGeocodeResponse findNearestPlaceFallback(
            KakaoCoordToRegionCodeResponse.Document regionDoc,
            String regionAddress,
            double lat,
            double lng
    ) {
        String query = null;
        if (regionDoc != null && hasText(regionDoc.region3DepthName())) {
            query = regionDoc.region3DepthName();
        } else if (hasText(regionAddress)) {
            query = regionAddress;
        }

        if (!hasText(query)) {
            return null;
        }

        final String fallbackQuery = query;

        KakaoCategorySearchResponse placeResp = kakaoLocalClient.searchKeywordByDistance(
                        fallbackQuery, lat, lng, 500, 1, 1
                )
                .doOnSubscribe(s -> log.info("Kakao nearest place fallback start query='{}', lat={}, lng={}", fallbackQuery, lat, lng))
                .doOnError(e -> log.error("Kakao nearest place fallback fail query='{}', lat={}, lng={}", fallbackQuery, lat, lng, e))
                .onErrorReturn(new KakaoCategorySearchResponse(null, List.of()))
                .block();

        if (placeResp == null || placeResp.documents() == null || placeResp.documents().isEmpty()) {
            return null;
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument place = placeResp.documents().getFirst();
        Long distanceMeters = parseDistance(place.distance());

        String resolvedAddress = normalizeAddress(place.addressName());

        String resolvedRoadAddress = resolveRoadAddress(
                place.addressName(),
                place.roadAddressName(),
                lat,
                lng
        );

        String normalizedRegionAddress = normalizeAddress(regionAddress);

        return ReverseGeocodeResponse.ofNearestPlace(
                resolvedAddress,
                resolvedRoadAddress,
                normalizedRegionAddress,
                place.placeName(),
                distanceMeters,
                lat,
                lng
        );
    }

    private String normalizeAddress(String address) {
        if (!hasText(address)) {
            return address;
        }

        String normalized = address.trim().replaceAll("\\s+", " ");

        if (normalized.startsWith("서울 ")) {
            return "서울특별시 " + normalized.substring("서울 ".length());
        }
        if (normalized.startsWith("부산 ")) {
            return "부산광역시 " + normalized.substring("부산 ".length());
        }
        if (normalized.startsWith("대구 ")) {
            return "대구광역시 " + normalized.substring("대구 ".length());
        }
        if (normalized.startsWith("인천 ")) {
            return "인천광역시 " + normalized.substring("인천 ".length());
        }
        if (normalized.startsWith("광주 ")) {
            return "광주광역시 " + normalized.substring("광주 ".length());
        }
        if (normalized.startsWith("대전 ")) {
            return "대전광역시 " + normalized.substring("대전 ".length());
        }
        if (normalized.startsWith("울산 ")) {
            return "울산광역시 " + normalized.substring("울산 ".length());
        }
        if (normalized.startsWith("세종 ")) {
            return "세종특별자치시 " + normalized.substring("세종 ".length());
        }
        if (normalized.startsWith("경기 ")) {
            return "경기도 " + normalized.substring("경기 ".length());
        }
        if (normalized.startsWith("강원 ")) {
            return "강원특별자치도 " + normalized.substring("강원 ".length());
        }
        if (normalized.startsWith("충북 ")) {
            return "충청북도 " + normalized.substring("충북 ".length());
        }
        if (normalized.startsWith("충남 ")) {
            return "충청남도 " + normalized.substring("충남 ".length());
        }
        if (normalized.startsWith("전북 ")) {
            return "전북특별자치도 " + normalized.substring("전북 ".length());
        }
        if (normalized.startsWith("전남 ")) {
            return "전라남도 " + normalized.substring("전남 ".length());
        }
        if (normalized.startsWith("경북 ")) {
            return "경상북도 " + normalized.substring("경북 ".length());
        }
        if (normalized.startsWith("경남 ")) {
            return "경상남도 " + normalized.substring("경남 ".length());
        }
        if (normalized.startsWith("제주 ")) {
            return "제주특별자치도 " + normalized.substring("제주 ".length());
        }

        return normalized;
    }

    private String resolveRoadAddress(
            String jibunAddress,
            String roadAddress,
            double lat,
            double lng
    ) {
        if (hasText(roadAddress)) {
            return normalizeAddress(roadAddress);
        }

        String convertedRoadAddress = convertJibunToRoadAddress(jibunAddress, lat, lng);
        if (hasText(convertedRoadAddress)) {
            return normalizeAddress(convertedRoadAddress);
        }

        // 카카오에서 도로명 주소가 존재하지 않는 장소도 있으므로,
        // 빈 값 방지를 위해 마지막에는 지번 주소를 반환한다.
        return hasText(jibunAddress) ? normalizeAddress(jibunAddress) : null;
    }

    private String convertJibunToRoadAddress(
            String jibunAddress,
            double lat,
            double lng
    ) {
        if (!hasText(jibunAddress)) {
            return null;
        }

        KakaoAddressSearchResponse resp = kakaoLocalClient.searchAddress(jibunAddress.trim())
                .doOnSubscribe(s -> log.info(
                        "Kakao jibun to road address start address='{}'",
                        jibunAddress
                ))
                .doOnError(e -> log.warn(
                        "Kakao jibun to road address fail address='{}'",
                        jibunAddress,
                        e
                ))
                .onErrorReturn(new KakaoAddressSearchResponse(null, List.of()))
                .block();

        return selectNearestRoadAddress(resp, lat, lng);
    }

    private String selectNearestRoadAddress(
            KakaoAddressSearchResponse resp,
            double lat,
            double lng
    ) {
        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
            return null;
        }

        String fallbackRoadAddress = null;
        Long minDistance = null;
        String nearestRoadAddress = null;

        for (KakaoAddressSearchResponse.Document doc : resp.documents()) {
            if (doc.roadAddress() == null || !hasText(doc.roadAddress().addressName())) {
                continue;
            }

            String roadAddress = doc.roadAddress().addressName().trim();

            if (fallbackRoadAddress == null) {
                fallbackRoadAddress = roadAddress;
            }

            Double docLng = parseDoubleOrNull(doc.roadAddress().x());
            Double docLat = parseDoubleOrNull(doc.roadAddress().y());

            if (docLng == null || docLat == null) {
                docLng = parseDoubleOrNull(doc.x());
                docLat = parseDoubleOrNull(doc.y());
            }

            if (docLng == null || docLat == null) {
                continue;
            }

            long distance = haversineMeters(lat, lng, docLat, docLng);

            if (minDistance == null || distance < minDistance) {
                minDistance = distance;
                nearestRoadAddress = roadAddress;
            }
        }

        return hasText(nearestRoadAddress) ? nearestRoadAddress : fallbackRoadAddress;
    }

    private static Double parseDoubleOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }
    }

    private void validateLatLng(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
        if (lng < -180 || lng > 180) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
    }

    private static long haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (long) (R * c);
    }

    private static String toExternalPlaceId(String provider, String externalId) {
        return "ext:" + provider + ":" + externalId;
    }

    private static Integer toDirectionClock(double originLat, double originLng, double targetLat, double targetLng) {
        double bearing = bearingDegrees(originLat, originLng, targetLat, targetLng);

        // 0도=북(12시), 90도=동(3시) ... 30도 단위로 반올림
        return (int) Math.floor((bearing + 15.0) / 30.0) % 12;
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

    private static Long parseDistance(String distance) {
        if (!hasText(distance)) {
            return null;
        }

        try {
            return Long.parseLong(distance);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}