package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.*;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToRegionCodeResponse;
import com.example.capstone.domain.place.dto.response.naver.NaverAddressCandidate;
import com.example.capstone.domain.place.dto.response.naver.NaverGeocodeResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;
    private final NaverMapClient naverMapClient;
    private final PlaceAddressResolver placeAddressResolver;
    private final PlaceCache placeCache;

    private static final int MAX_PAGE = 45;
    private static final int MAX_SIZE = 15;
    private static final List<Integer> NEAREST_PLACE_FALLBACK_RADII_M = List.of(5, 10, 15);
    private static final int NEAREST_PLACE_SEARCH_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(
            KakaoLocalClient kakaoLocalClient,
            NaverMapClient naverMapClient,
            PlaceCache placeCache,
            PlaceAddressResolver placeAddressResolver) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.naverMapClient = naverMapClient;
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
            KakaoCategorySearchResponse resp = kakaoLocalClient.searchKeywordByAccuracy(
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

    private record ReverseAddressCandidate(
            String roadAddress,
            String jibunAddress,
            String source,
            Double lat,
            Double lng,
            Long distanceM
    ) {
        String finalAddress() {
            return hasText(roadAddress) ? roadAddress : jibunAddress;
        }
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
            ReverseAddressCandidate kakaoJibunCandidate = null;
            String coordRoadName = null;

            KakaoCoordToAddressResponse addressResp = kakaoLocalClient.coordToAddress(lat, lng)
                    .doOnSubscribe(s -> log.info("Kakao reverse geocode start lat={}, lng={}", lat, lng))
                    .doOnError(e -> log.error("Kakao reverse geocode fail lat={}, lng={}", lat, lng, e))
                    .block();

            if (addressResp != null && addressResp.documents() != null && !addressResp.documents().isEmpty()) {
                KakaoCoordToAddressResponse.Document doc = addressResp.documents().getFirst();

                String jibunAddress = doc.address() != null ? doc.address().addressName() : null;
                String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;
                coordRoadName = doc.roadAddress() != null ? doc.roadAddress().roadName() : null;

                String normalizedJibunAddress = placeAddressResolver.normalizeAddress(jibunAddress);
                String normalizedRoadAddress = placeAddressResolver.normalizeAddress(roadAddress);

                if (hasText(normalizedRoadAddress)) {
                    return ReverseGeocodeResponse.ofAddress(
                            normalizedRoadAddress,
                            normalizedJibunAddress,
                            lat,
                            lng
                    );
                }

                if (hasText(normalizedJibunAddress)) {
                    kakaoJibunCandidate = new ReverseAddressCandidate(
                            null,
                            normalizedJibunAddress,
                            "KAKAO_JIBUN_ADDRESS",
                            null,
                            null,
                            null
                    );
                }
            }

            if (kakaoJibunCandidate != null) {
                ReverseAddressCandidate naverCandidate =
                        findNaverReverseGeocodeCandidate(lat, lng);

                if (naverCandidate != null) {
                    ReverseAddressCandidate enrichedKakao =
                            enrichKakaoCandidateDistance(kakaoJibunCandidate, lat, lng);

                    ReverseAddressCandidate enrichedNaver =
                            enrichNaverCandidateDistance(naverCandidate, lat, lng);

                    ReverseAddressCandidate selected =
                            selectBetterReverseAddressCandidate(enrichedKakao, enrichedNaver);

                    ReverseGeocodeResponse selectedResponse =
                            toReverseGeocodeResponse(selected, lat, lng);

                    if (selectedResponse != null) {
                        return selectedResponse;
                    }
                }

                return ReverseGeocodeResponse.ofAddress(
                        null,
                        kakaoJibunCandidate.jibunAddress(),
                        lat,
                        lng
                );
            }

            ReverseAddressCandidate naverCandidate =
                    findNaverReverseGeocodeCandidate(lat, lng);

            if (naverCandidate != null) {
                ReverseGeocodeResponse naverResponse =
                        toReverseGeocodeResponse(naverCandidate, lat, lng);

                if (naverResponse != null) {
                    return naverResponse;
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

            List<String> fallbackQueries = buildReverseGeocodeFallbackQueries(
                    coordRoadName,
                    regionResp
            );

            ReverseGeocodeResponse nearestPlace = findNearestPlaceFallback(
                    fallbackQueries,
                    regionAddress,
                    lat,
                    lng
            );

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

    private ReverseAddressCandidate enrichKakaoCandidateDistance(
            ReverseAddressCandidate candidate,
            double originLat,
            double originLng
    ) {
        if (candidate == null || !hasText(candidate.finalAddress())) {
            return candidate;
        }

        KakaoAddressSearchResponse resp = kakaoLocalClient.searchAddress(candidate.finalAddress())
                .doOnSubscribe(s -> log.info(
                        "[kakao candidate geocode] source={}, address='{}'",
                        candidate.source(),
                        candidate.finalAddress()
                ))
                .doOnError(e -> log.error(
                        "[kakao candidate geocode fail] source={}, address='{}'",
                        candidate.source(),
                        candidate.finalAddress(),
                        e
                ))
                .onErrorReturn(new KakaoAddressSearchResponse(null, List.of()))
                .block();

        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
            return candidate;
        }

        KakaoAddressSearchResponse.Document doc = resp.documents().getFirst();

        try {
            double candidateLng = Double.parseDouble(doc.x());
            double candidateLat = Double.parseDouble(doc.y());
            long distanceM = haversineMeters(originLat, originLng, candidateLat, candidateLng);

            return new ReverseAddressCandidate(
                    candidate.roadAddress(),
                    candidate.jibunAddress(),
                    candidate.source(),
                    candidateLat,
                    candidateLng,
                    distanceM
            );
        } catch (Exception e) {
            return candidate;
        }
    }

    private ReverseAddressCandidate enrichNaverCandidateDistance(
            ReverseAddressCandidate candidate,
            double originLat,
            double originLng
    ) {
        if (candidate == null || !hasText(candidate.finalAddress())) {
            return candidate;
        }

        NaverGeocodeResponse resp = naverMapClient.geocode(candidate.finalAddress())
                .doOnSubscribe(s -> log.info(
                        "[naver candidate geocode] source={}, address='{}'",
                        candidate.source(),
                        candidate.finalAddress()
                ))
                .doOnError(e -> log.error(
                        "[naver candidate geocode fail] source={}, address='{}'",
                        candidate.source(),
                        candidate.finalAddress(),
                        e
                ))
                .onErrorReturn(new NaverGeocodeResponse("ERROR", null, List.of(), "fallback failed"))
                .block();

        if (resp == null || resp.addresses() == null || resp.addresses().isEmpty()) {
            return candidate;
        }

        NaverGeocodeResponse.Address address = resp.addresses().getFirst();

        try {
            double candidateLng = Double.parseDouble(address.x());
            double candidateLat = Double.parseDouble(address.y());
            long distanceM = haversineMeters(originLat, originLng, candidateLat, candidateLng);

            return new ReverseAddressCandidate(
                    candidate.roadAddress(),
                    candidate.jibunAddress(),
                    candidate.source(),
                    candidateLat,
                    candidateLng,
                    distanceM
            );
        } catch (Exception e) {
            return candidate;
        }
    }

    private ReverseAddressCandidate selectBetterReverseAddressCandidate(
            ReverseAddressCandidate kakaoCandidate,
            ReverseAddressCandidate naverCandidate
    ) {
        if (kakaoCandidate == null) {
            return naverCandidate;
        }

        if (naverCandidate == null) {
            return kakaoCandidate;
        }

        if (kakaoCandidate.distanceM() != null && naverCandidate.distanceM() != null) {
            if (!kakaoCandidate.distanceM().equals(naverCandidate.distanceM())) {
                return kakaoCandidate.distanceM() < naverCandidate.distanceM()
                        ? kakaoCandidate
                        : naverCandidate;
            }
        }

        if (kakaoCandidate.distanceM() != null && naverCandidate.distanceM() == null) {
            return kakaoCandidate;
        }

        if (kakaoCandidate.distanceM() == null && naverCandidate.distanceM() != null) {
            return naverCandidate;
        }

        return sourcePriority(kakaoCandidate.source()) <= sourcePriority(naverCandidate.source())
                ? kakaoCandidate
                : naverCandidate;
    }

    private int sourcePriority(String source) {
        return switch (source) {
            case "NAVER_ROAD_ADDRESS" -> 1;
            case "KAKAO_JIBUN_ADDRESS" -> 2;
            case "NAVER_JIBUN_ADDRESS" -> 3;
            default -> 99;
        };
    }

    private ReverseGeocodeResponse toReverseGeocodeResponse(
            ReverseAddressCandidate candidate,
            double lat,
            double lng
    ) {
        if (candidate == null || !hasText(candidate.finalAddress())) {
            return null;
        }

        return ReverseGeocodeResponse.ofResolvedAddress(
                candidate.finalAddress(),
                candidate.source(),
                lat,
                lng
        );
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

    private ReverseAddressCandidate findNaverReverseGeocodeCandidate(
            double lat,
            double lng
    ) {
        NaverAddressCandidate naverCandidate = naverMapClient.reverseGeocode(lat, lng)
                .doOnSubscribe(s -> log.info("Naver reverse geocode start lat={}, lng={}", lat, lng))
                .doOnError(e -> log.error("Naver reverse geocode fail lat={}, lng={}", lat, lng, e))
                .onErrorReturn(new NaverAddressCandidate(null, null))
                .block();

        if (naverCandidate == null) {
            return null;
        }

        String roadAddress = placeAddressResolver.normalizeAddress(naverCandidate.roadAddress());
        String jibunAddress = placeAddressResolver.normalizeAddress(naverCandidate.jibunAddress());

        if (hasText(roadAddress)) {
            return new ReverseAddressCandidate(
                    roadAddress,
                    jibunAddress,
                    "NAVER_ROAD_ADDRESS",
                    null,
                    null,
                    null
            );
        }

        if (hasText(jibunAddress)) {
            return new ReverseAddressCandidate(
                    null,
                    jibunAddress,
                    "NAVER_JIBUN_ADDRESS",
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    private ReverseGeocodeResponse findNearestPlaceFallback(
            List<String> fallbackQueries,
            String regionAddress,
            double lat,
            double lng
    ) {
        if (fallbackQueries == null || fallbackQueries.isEmpty()) {
            return null;
        }

        for (String fallbackQuery : fallbackQueries) {
            for (int radiusM : NEAREST_PLACE_FALLBACK_RADII_M) {
                KakaoCategorySearchResponse placeResp = kakaoLocalClient.searchKeywordByDistance(
                                fallbackQuery,
                                lat,
                                lng,
                                radiusM,
                                NEAREST_PLACE_SEARCH_SIZE,
                                1
                        )
                        .doOnSubscribe(s -> log.info(
                                "Kakao nearest address fallback start query='{}', lat={}, lng={}, radius={}m",
                                fallbackQuery,
                                lat,
                                lng,
                                radiusM
                        ))
                        .doOnError(e -> log.error(
                                "Kakao nearest address fallback fail query='{}', lat={}, lng={}, radius={}m",
                                fallbackQuery,
                                lat,
                                lng,
                                radiusM,
                                e
                        ))
                        .onErrorReturn(new KakaoCategorySearchResponse(null, List.of()))
                        .block();

                logNearestPlaceFallbackCandidates(fallbackQuery, radiusM, placeResp);

                KakaoCategorySearchResponse.KakaoPlaceDocument nearestPlace =
                        selectNearestAddressPlace(placeResp, radiusM);

                if (nearestPlace == null) {
                    continue;
                }

                Long distanceM = parseDistance(nearestPlace.distance());
                if (distanceM == null) {
                    continue;
                }

                String normalizedRoadAddress = placeAddressResolver.normalizeAddress(
                        nearestPlace.roadAddressName()
                );

                String normalizedJibunAddress = placeAddressResolver.normalizeAddress(
                        nearestPlace.addressName()
                );

                String nearestAddress = hasText(normalizedRoadAddress)
                        ? normalizedRoadAddress
                        : normalizedJibunAddress;

                if (!hasText(nearestAddress)) {
                    continue;
                }

                String normalizedRegionAddress = placeAddressResolver.normalizeAddress(regionAddress);

                return ReverseGeocodeResponse.ofNearestPlace(
                        normalizedRoadAddress,
                        normalizedJibunAddress,
                        normalizedRegionAddress,
                        nearestPlace.placeName(),
                        distanceM,
                        lat,
                        lng
                );
            }
        }

        return null;
    }

    private void logNearestPlaceFallbackCandidates(
            String query,
            int radiusM,
            KakaoCategorySearchResponse placeResp
    ) {
        int resultCount = placeResp == null || placeResp.documents() == null
                ? 0
                : placeResp.documents().size();

        log.info(
                "[nearest fallback] query='{}', radius={}m, resultCount={}",
                query,
                radiusM,
                resultCount
        );

        if (placeResp == null || placeResp.documents() == null) {
            return;
        }

        for (KakaoCategorySearchResponse.KakaoPlaceDocument place : placeResp.documents()) {
            log.info(
                    "[nearest fallback candidate] query='{}', radius={}m, name='{}', road='{}', jibun='{}', distance='{}', lat={}, lng={}",
                    query,
                    radiusM,
                    place.placeName(),
                    place.roadAddressName(),
                    place.addressName(),
                    place.distance(),
                    place.y(),
                    place.x()
            );
        }
    }

    private KakaoCategorySearchResponse.KakaoPlaceDocument selectNearestAddressPlace(
            KakaoCategorySearchResponse placeResp,
            int radiusM
    ) {
        if (placeResp == null || placeResp.documents() == null || placeResp.documents().isEmpty()) {
            return null;
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument nearest = null;
        Long nearestDistance = null;

        for (KakaoCategorySearchResponse.KakaoPlaceDocument place : placeResp.documents()) {
            Long distanceM = parseDistance(place.distance());

            if (distanceM == null || distanceM > radiusM) {
                continue;
            }

            if (!hasText(place.roadAddressName()) && !hasText(place.addressName())) {
                continue;
            }

            if (nearestDistance == null || distanceM < nearestDistance) {
                nearest = place;
                nearestDistance = distanceM;
            }
        }

        return nearest;
    }

    private List<String> buildReverseGeocodeFallbackQueries(
            String roadName,
            KakaoCoordToRegionCodeResponse regionResp
    ) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        // 1. 도로명 일부: 예) 솔샘로16길
        addIfHasText(queries, roadName);

        KakaoCoordToRegionCodeResponse.Document legalRegionDoc =
                selectRegionDocumentByType(regionResp, "B");

        KakaoCoordToRegionCodeResponse.Document administrativeRegionDoc =
                selectRegionDocumentByType(regionResp, "H");

        // 2. 지번/법정동: 예) 정릉동
        if (legalRegionDoc != null) {
            addIfHasText(queries, legalRegionDoc.region3DepthName());
        }

        // 3. 행정동: 예) 정릉3동
        if (administrativeRegionDoc != null) {
            addIfHasText(queries, administrativeRegionDoc.region3DepthName());
        }

        // 4. 구 단위 포함: 예) 성북구 정릉동
        if (legalRegionDoc != null
                && hasText(legalRegionDoc.region2DepthName())
                && hasText(legalRegionDoc.region3DepthName())) {
            addIfHasText(
                    queries,
                    legalRegionDoc.region2DepthName() + " " + legalRegionDoc.region3DepthName()
            );
        }

        return new ArrayList<>(queries);
    }

    private KakaoCoordToRegionCodeResponse.Document selectRegionDocumentByType(
            KakaoCoordToRegionCodeResponse regionResp,
            String regionType
    ) {
        if (regionResp == null || regionResp.documents() == null || regionResp.documents().isEmpty()) {
            return null;
        }

        return regionResp.documents().stream()
                .filter(doc -> regionType.equals(doc.regionType()))
                .findFirst()
                .orElse(null);
    }

    private static void addIfHasText(LinkedHashSet<String> values, String value) {
        if (hasText(value)) {
            values.add(value.trim());
        }
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