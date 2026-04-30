package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.ReverseGeocodeResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToRegionCodeResponse;
import com.example.capstone.domain.place.dto.response.naver.NaverAddressCandidate;
import com.example.capstone.domain.place.dto.response.naver.NaverGeocodeResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceReverseGeocodeResolver {

    private static final List<Integer> NEAREST_PLACE_FALLBACK_RADII_M = List.of(5, 10, 15);
    private static final int NEAREST_PLACE_SEARCH_SIZE = 5;

    private static final String SOURCE_KAKAO_JIBUN = "KAKAO_JIBUN_ADDRESS";
    private static final String SOURCE_NAVER_ROAD = "NAVER_ROAD_ADDRESS";
    private static final String SOURCE_NAVER_JIBUN = "NAVER_JIBUN_ADDRESS";

    private final KakaoLocalClient kakaoLocalClient;
    private final NaverMapClient naverMapClient;
    private final PlaceAddressResolver placeAddressResolver;
    private final PlaceGeoCalculator placeGeoCalculator;

    public ReverseGeocodeResponse resolve(double lat, double lng) {
        KakaoReverseAddressResult kakaoResult = findKakaoReverseAddress(lat, lng);

        if (kakaoResult.hasRoadAddress()) {
            return ReverseGeocodeResponse.ofAddress(
                    kakaoResult.roadAddress(),
                    kakaoResult.jibunAddress(),
                    lat,
                    lng
            );
        }

        if (kakaoResult.hasJibunAddress()) {
            ReverseGeocodeResponse selected = resolveWithKakaoJibunAndNaver(
                    kakaoResult.toJibunCandidate(),
                    lat,
                    lng
            );

            if (selected != null) {
                return selected;
            }

            return ReverseGeocodeResponse.ofAddress(
                    null,
                    kakaoResult.jibunAddress(),
                    lat,
                    lng
            );
        }

        ReverseGeocodeResponse naverResponse = resolveByNaverOnly(lat, lng);
        if (naverResponse != null) {
            return naverResponse;
        }

        return resolveByRegionAndNearestPlaceFallback(
                kakaoResult.roadName(),
                lat,
                lng
        );
    }

    private KakaoReverseAddressResult findKakaoReverseAddress(double lat, double lng) {
        KakaoCoordToAddressResponse response = kakaoLocalClient.coordToAddress(lat, lng)
                .doOnSubscribe(s -> log.info("Kakao reverse geocode start lat={}, lng={}", lat, lng))
                .doOnError(e -> log.error("Kakao reverse geocode fail lat={}, lng={}", lat, lng, e))
                .block();

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return KakaoReverseAddressResult.empty();
        }

        KakaoCoordToAddressResponse.Document document = response.documents().getFirst();

        String jibunAddress = document.address() != null
                ? placeAddressResolver.normalizeAddress(document.address().addressName())
                : null;

        String roadAddress = document.roadAddress() != null
                ? placeAddressResolver.normalizeAddress(document.roadAddress().addressName())
                : null;

        String roadName = document.roadAddress() != null
                ? document.roadAddress().roadName()
                : null;

        return new KakaoReverseAddressResult(roadAddress, jibunAddress, roadName);
    }

    private ReverseGeocodeResponse resolveWithKakaoJibunAndNaver(
            ReverseAddressCandidate kakaoCandidate,
            double lat,
            double lng
    ) {
        ReverseAddressCandidate naverCandidate = findNaverReverseGeocodeCandidate(lat, lng);
        if (naverCandidate == null) {
            return null;
        }

        ReverseAddressCandidate enrichedKakao = enrichKakaoCandidateDistance(
                kakaoCandidate,
                lat,
                lng
        );

        ReverseAddressCandidate enrichedNaver = enrichNaverCandidateDistance(
                naverCandidate,
                lat,
                lng
        );

        ReverseAddressCandidate selected = selectBetterReverseAddressCandidate(
                enrichedKakao,
                enrichedNaver
        );

        return toReverseGeocodeResponse(selected, lat, lng);
    }

    private ReverseGeocodeResponse resolveByNaverOnly(double lat, double lng) {
        ReverseAddressCandidate naverCandidate = findNaverReverseGeocodeCandidate(lat, lng);
        return toReverseGeocodeResponse(naverCandidate, lat, lng);
    }

    private ReverseGeocodeResponse resolveByRegionAndNearestPlaceFallback(
            String roadName,
            double lat,
            double lng
    ) {
        KakaoCoordToRegionCodeResponse regionResponse = kakaoLocalClient.coordToRegionCode(lat, lng)
                .doOnSubscribe(s -> log.info("Kakao coord2region start lat={}, lng={}", lat, lng))
                .doOnError(e -> log.error("Kakao coord2region fail lat={}, lng={}", lat, lng, e))
                .block();

        KakaoCoordToRegionCodeResponse.Document regionDocument = selectRegionDocument(regionResponse);
        String regionAddress = regionDocument != null
                ? placeAddressResolver.normalizeAddress(regionDocument.addressName())
                : null;

        List<String> fallbackQueries = buildReverseGeocodeFallbackQueries(
                roadName,
                regionResponse
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
                    SOURCE_NAVER_ROAD,
                    null,
                    null,
                    null
            );
        }

        if (hasText(jibunAddress)) {
            return new ReverseAddressCandidate(
                    null,
                    jibunAddress,
                    SOURCE_NAVER_JIBUN,
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    private ReverseAddressCandidate enrichKakaoCandidateDistance(
            ReverseAddressCandidate candidate,
            double originLat,
            double originLng
    ) {
        if (candidate == null || !hasText(candidate.finalAddress())) {
            return candidate;
        }

        KakaoAddressSearchResponse response = kakaoLocalClient.searchAddress(candidate.finalAddress())
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

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return candidate;
        }

        KakaoAddressSearchResponse.Document document = response.documents().getFirst();

        Double candidateLng = placeGeoCalculator.parseDoubleOrNull(document.x());
        Double candidateLat = placeGeoCalculator.parseDoubleOrNull(document.y());

        if (candidateLat == null || candidateLng == null) {
            return candidate;
        }

        long distanceM = placeGeoCalculator.haversineMeters(
                originLat,
                originLng,
                candidateLat,
                candidateLng
        );

        return candidate.withDistance(candidateLat, candidateLng, distanceM);
    }

    private ReverseAddressCandidate enrichNaverCandidateDistance(
            ReverseAddressCandidate candidate,
            double originLat,
            double originLng
    ) {
        if (candidate == null || !hasText(candidate.finalAddress())) {
            return candidate;
        }

        NaverGeocodeResponse response = naverMapClient.geocode(candidate.finalAddress())
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

        if (response == null || response.addresses() == null || response.addresses().isEmpty()) {
            return candidate;
        }

        NaverGeocodeResponse.Address address = response.addresses().getFirst();

        Double candidateLng = placeGeoCalculator.parseDoubleOrNull(address.x());
        Double candidateLat = placeGeoCalculator.parseDoubleOrNull(address.y());

        if (candidateLat == null || candidateLng == null) {
            return candidate;
        }

        long distanceM = placeGeoCalculator.haversineMeters(
                originLat,
                originLng,
                candidateLat,
                candidateLng
        );

        return candidate.withDistance(candidateLat, candidateLng, distanceM);
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

        if (kakaoCandidate.distanceM() != null && naverCandidate.distanceM() != null
                && !kakaoCandidate.distanceM().equals(naverCandidate.distanceM())) {
            return kakaoCandidate.distanceM() < naverCandidate.distanceM()
                    ? kakaoCandidate
                    : naverCandidate;
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
            case SOURCE_NAVER_ROAD -> 1;
            case SOURCE_KAKAO_JIBUN -> 2;
            case SOURCE_NAVER_JIBUN -> 3;
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
                KakaoCategorySearchResponse placeResponse = kakaoLocalClient.searchKeywordByDistance(
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

                logNearestPlaceFallbackCandidates(fallbackQuery, radiusM, placeResponse);

                KakaoCategorySearchResponse.KakaoPlaceDocument nearestPlace =
                        selectNearestAddressPlace(placeResponse, radiusM);

                if (nearestPlace == null) {
                    continue;
                }

                Long distanceM = placeGeoCalculator.parseDistance(nearestPlace.distance());
                if (distanceM == null) {
                    continue;
                }

                String normalizedRoadAddress = placeAddressResolver.normalizeAddress(
                        nearestPlace.roadAddressName()
                );

                String normalizedJibunAddress = placeAddressResolver.normalizeAddress(
                        nearestPlace.addressName()
                );

                if (!hasText(normalizedRoadAddress) && !hasText(normalizedJibunAddress)) {
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

    private KakaoCategorySearchResponse.KakaoPlaceDocument selectNearestAddressPlace(
            KakaoCategorySearchResponse placeResponse,
            int radiusM
    ) {
        if (placeResponse == null || placeResponse.documents() == null || placeResponse.documents().isEmpty()) {
            return null;
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument nearest = null;
        Long nearestDistance = null;

        for (KakaoCategorySearchResponse.KakaoPlaceDocument place : placeResponse.documents()) {
            Long distanceM = placeGeoCalculator.parseDistance(place.distance());

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

    private void logNearestPlaceFallbackCandidates(
            String query,
            int radiusM,
            KakaoCategorySearchResponse placeResponse
    ) {
        int resultCount = placeResponse == null || placeResponse.documents() == null
                ? 0
                : placeResponse.documents().size();

        log.info(
                "[nearest fallback] query='{}', radius={}m, resultCount={}",
                query,
                radiusM,
                resultCount
        );

        if (placeResponse == null || placeResponse.documents() == null) {
            return;
        }

        for (KakaoCategorySearchResponse.KakaoPlaceDocument place : placeResponse.documents()) {
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

    private List<String> buildReverseGeocodeFallbackQueries(
            String roadName,
            KakaoCoordToRegionCodeResponse regionResponse
    ) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        addIfHasText(queries, roadName);

        KakaoCoordToRegionCodeResponse.Document legalRegionDocument =
                selectRegionDocumentByType(regionResponse, "B");

        KakaoCoordToRegionCodeResponse.Document administrativeRegionDocument =
                selectRegionDocumentByType(regionResponse, "H");

        if (legalRegionDocument != null) {
            addIfHasText(queries, legalRegionDocument.region3DepthName());
        }

        if (administrativeRegionDocument != null) {
            addIfHasText(queries, administrativeRegionDocument.region3DepthName());
        }

        if (legalRegionDocument != null
                && hasText(legalRegionDocument.region2DepthName())
                && hasText(legalRegionDocument.region3DepthName())) {
            addIfHasText(
                    queries,
                    legalRegionDocument.region2DepthName() + " " + legalRegionDocument.region3DepthName()
            );
        }

        return new ArrayList<>(queries);
    }

    private KakaoCoordToRegionCodeResponse.Document selectRegionDocument(
            KakaoCoordToRegionCodeResponse regionResponse
    ) {
        if (regionResponse == null || regionResponse.documents() == null || regionResponse.documents().isEmpty()) {
            return null;
        }

        return regionResponse.documents().stream()
                .filter(document -> "H".equals(document.regionType()))
                .findFirst()
                .or(() -> regionResponse.documents().stream()
                        .filter(document -> "B".equals(document.regionType()))
                        .findFirst())
                .orElse(regionResponse.documents().getFirst());
    }

    private KakaoCoordToRegionCodeResponse.Document selectRegionDocumentByType(
            KakaoCoordToRegionCodeResponse regionResponse,
            String regionType
    ) {
        if (regionResponse == null || regionResponse.documents() == null || regionResponse.documents().isEmpty()) {
            return null;
        }

        return regionResponse.documents().stream()
                .filter(document -> regionType.equals(document.regionType()))
                .findFirst()
                .orElse(null);
    }

    private void addIfHasText(LinkedHashSet<String> values, String value) {
        if (hasText(value)) {
            values.add(value.trim());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record KakaoReverseAddressResult(
            String roadAddress,
            String jibunAddress,
            String roadName
    ) {
        static KakaoReverseAddressResult empty() {
            return new KakaoReverseAddressResult(null, null, null);
        }

        boolean hasRoadAddress() {
            return hasText(roadAddress);
        }

        boolean hasJibunAddress() {
            return hasText(jibunAddress);
        }

        ReverseAddressCandidate toJibunCandidate() {
            return new ReverseAddressCandidate(
                    null,
                    jibunAddress,
                    SOURCE_KAKAO_JIBUN,
                    null,
                    null,
                    null
            );
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
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

        ReverseAddressCandidate withDistance(Double lat, Double lng, Long distanceM) {
            return new ReverseAddressCandidate(
                    roadAddress,
                    jibunAddress,
                    source,
                    lat,
                    lng,
                    distanceM
            );
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}