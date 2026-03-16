package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.PlacePageResponse;
import com.example.capstone.domain.place.dto.response.PlaceResponse;
import com.example.capstone.domain.place.dto.response.PlaceDetailResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.global.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;
    private final PlaceCache placeCache;

    private static final int MAX_PAGE = 45;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceCache placeCache) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.placeCache = placeCache;
    }

    /**
     * NOTE:
     * - 이 메서드는 "전체 정렬 리스트"를 만들어 반환합니다.
     * - pagination(page/size)은 Controller에서 슬라이싱해서 처리하세요.
     */
    public List<PlaceResponse> findNearbyByCategoryCodes(
            double lat,
            double lng,
            int radiusM,
            List<String> categoryCodes,
            int sizePerCategory,
            int maxItems
    ) {
        List<PlaceResponse> merged = Flux.fromIterable(categoryCodes)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .flatMap(code -> fetchAllPagesByCategory(code, lat, lng, radiusM, sizePerCategory))
                .flatMapIterable(resp -> resp.documents() == null ? List.of() : resp.documents())
                .map(doc -> toNearbyPlace(doc, lat, lng))
                .collectList()
                .blockOptional()
                .orElse(List.of());

        // id 기준 중복 제거
        Map<String, PlaceResponse> dedup = new LinkedHashMap<>();
        for (PlaceResponse p : merged) {
            if (p != null && p.placeId() != null) {
                dedup.putIfAbsent(p.placeId(), p);
            }
        }

        // 거리(distance)가 null이면 직접 계산
        List<PlaceResponse> ranked = dedup.values().stream()
                .map(p -> p.distanceM() == null ? withDistance(p, lat, lng) : p)
                .filter(p -> p.distanceM() != null && p.distanceM() <= radiusM)
                // 거리(distance)가 같으면 id로 tie-break
                .sorted(
                        Comparator.comparingLong(PlaceResponse::distanceM)
                                .thenComparing(PlaceResponse::placeId)
                )
                .toList();

        // (기존 호환용) maxItems 제한
        if (maxItems > 0 && ranked.size() > maxItems) {
            return ranked.subList(0, maxItems);
        }
        return ranked;
    }

    public PlacePageResponse searchPlaces(
            String query,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("BAD_REQUEST", "query is required");
        }

        int kakaoPage = Math.min(Math.max(page, 1), MAX_PAGE);
        int kakaoSize = Math.min(Math.max(size, 1), 15); // Kakao limit

        KakaoCategorySearchResponse resp = kakaoLocalClient.searchKeyword(
                        query.trim(), lat, lng, radiusM, kakaoSize, kakaoPage
                )
                .doOnSubscribe(s -> log.info("Kakao keyword start query='{}', page={}, size={}, lat={}, lng={}, radius={}",
                        query, kakaoPage, kakaoSize, lat, lng, radiusM))
                .doOnError(e -> log.error("Kakao keyword fail query='{}'", query, e))
                .block();

        if (resp == null) {
            return new PlacePageResponse(List.of(), page, kakaoSize, 0, false);
        }

        int total = resp.meta() == null ? 0 : (int) resp.meta().pageableCount();
        boolean hasNext = resp.meta() != null && !resp.meta().isEnd();

        List<PlaceResponse> items =
                (resp.documents() == null ? List.<KakaoCategorySearchResponse.KakaoPlaceDocument>of() : resp.documents())
                        .stream()
                        .map(doc -> toSearchPlace(doc, lat, lng))
                        .toList();

        return new PlacePageResponse(items, page, kakaoSize, total, hasNext);
    }

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new BusinessException("BAD_REQUEST", "placeId is required");
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument doc = placeCache.get(placeId)
                .orElseThrow(() -> new BusinessException("PLACE_NOT_FOUND", "place not found in cache: " + placeId));

        double placeLng = Double.parseDouble(doc.x());
        double placeLat = Double.parseDouble(doc.y());
        Long dist = (doc.distance() == null || doc.distance().isBlank()) ? null : Long.parseLong(doc.distance());

        return new PlaceDetailResponse(
                placeId,
                doc.placeName(),
                doc.categoryName(),
                doc.addressName(),
                doc.roadAddressName(),
                placeLat,
                placeLng,
                dist,
                doc.phone(),
                doc.placeUrl(),
                null, // openingHours: Kakao Local REST에서 제공되지 않음
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

        return new PlaceResponse(
                placeId,
                d.placeName(),
                d.categoryName(),
                d.roadAddressName(),
                placeLat,
                placeLng,
                dist,
                directionClock
        );
    }

    private Flux<KakaoCategorySearchResponse> fetchAllPagesByCategory(
            String code,
            double lat,
            double lng,
            int radiusM,
            int sizePerCategory
    ) {
        return Flux.range(1, MAX_PAGE)
                .concatMap(page ->
                        kakaoLocalClient.searchCategory(code, lat, lng, radiusM, sizePerCategory, page)
                                .doOnSubscribe(s -> log.info(
                                        "Kakao category start code={}, page={}, lat={}, lng={}, radius={}",
                                        code, page, lat, lng, radiusM
                                ))
                                .doOnNext(resp -> log.info(
                                        "Kakao category ok code={}, page={}, total={}, docs={}, isEnd={}",
                                        code,
                                        page,
                                        resp.meta() == null ? -1 : resp.meta().totalCount(),
                                        resp.documents() == null ? 0 : resp.documents().size(),
                                        resp.meta() != null && resp.meta().isEnd()
                                ))
                                .doOnError(e -> log.error("Kakao category fail code={}, page={}", code, page, e))
                                .onErrorResume(e -> Mono.<KakaoCategorySearchResponse>empty())
                )
                // is_end=true 응답을 받는 순간 이후 페이지 호출 중단
                .takeUntil(resp -> resp.meta() != null && resp.meta().isEnd());
    }

    private PlaceResponse toNearbyPlace(
            KakaoCategorySearchResponse.KakaoPlaceDocument d,
            double originLat,
            double originLng
    ) {
        double placeLng = Double.parseDouble(d.x());
        double placeLat = Double.parseDouble(d.y());
        Long dist = (d.distance() == null || d.distance().isBlank()) ? null : Long.parseLong(d.distance());

        Integer directionClock = toDirectionClock(originLat, originLng, placeLat, placeLng);

        String placeId = toExternalPlaceId("KAKAO", d.id());
        placeCache.put(placeId, d);

        return new PlaceResponse(
                placeId,
                d.placeName(),
                d.categoryName(),
                d.roadAddressName(),
                placeLat,
                placeLng,
                dist,
                directionClock
        );
    }

    private PlaceResponse withDistance(PlaceResponse p, double originLat, double originLng) {
        long d = haversineMeters(originLat, originLng, p.lat(), p.lng());
        Integer directionClock = p.directionClock() != null ? p.directionClock() : toDirectionClock(originLat, originLng, p.lat(), p.lng());
        return new PlaceResponse(
                p.placeId(),
                p.name(),
                p.category(),
                p.roadAddress(),
                p.lat(),
                p.lng(),
                d,
                directionClock
        );
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
}