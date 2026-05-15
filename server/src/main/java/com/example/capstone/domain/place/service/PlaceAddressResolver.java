package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAddressResolver {

    private final KakaoLocalClient kakaoLocalClient;

    public String resolveRoadAddress(
            String jibunAddress,
            String roadAddress,
            Double lat,
            Double lng
    ) {
        return resolveRoadAddress(jibunAddress, roadAddress, lat, lng, new HashMap<>());
    }

    public String resolveRoadAddress(
            String jibunAddress,
            String roadAddress,
            Double lat,
            Double lng,
            Map<String, String> addressCache
    ) {
        if (hasText(roadAddress)) {
            return normalizeAddress(roadAddress);
        }

        String roadAddressByCoord = findRoadAddressByCoord(lat, lng);
        if (hasText(roadAddressByCoord)) {
            return normalizeAddress(roadAddressByCoord);
        }

        if (hasText(jibunAddress)) {
            String converted = convertJibunToRoadAddress(jibunAddress, lat, lng, addressCache);
            if (hasText(converted)) {
                return normalizeAddress(converted);
            }

            return normalizeAddress(jibunAddress);
        }

        return null;
    }

    public String resolveDisplayAddress(
            String address,
            Double lat,
            Double lng
    ) {
        if (!hasText(address)) {
            return null;
        }

        if (isLikelyRoadAddress(address)) {
            return normalizeAddress(address);
        }

        String roadAddressByCoord = findRoadAddressByCoord(lat, lng);
        if (hasText(roadAddressByCoord)) {
            return normalizeAddress(roadAddressByCoord);
        }

        String converted = convertJibunToRoadAddress(address, lat, lng, new HashMap<>());
        if (hasText(converted)) {
            return normalizeAddress(converted);
        }

        return normalizeAddress(address);
    }

    public String normalizeAddress(String address) {
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

    private String findRoadAddressByCoord(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }

        KakaoCoordToAddressResponse response = kakaoLocalClient.coordToAddress(lat, lng)
                .doOnSubscribe(s -> log.info("Kakao coord address conversion start lat={}, lng={}", lat, lng))
                .doOnError(e -> log.warn("Kakao coord address conversion fail lat={}, lng={}", lat, lng, e))
                .onErrorReturn(new KakaoCoordToAddressResponse(null, List.of()))
                .block();

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return null;
        }

        return response.documents().stream()
                .map(KakaoCoordToAddressResponse.Document::roadAddress)
                .filter(roadAddress -> roadAddress != null && hasText(roadAddress.addressName()))
                .map(KakaoCoordToAddressResponse.RoadAddress::addressName)
                .findFirst()
                .orElse(null);
    }

    private String convertJibunToRoadAddress(
            String jibunAddress,
            Double lat,
            Double lng,
            Map<String, String> addressCache
    ) {
        if (!hasText(jibunAddress)) {
            return null;
        }

        String key = normalizeAddress(jibunAddress);
        if (addressCache.containsKey(key)) {
            return addressCache.get(key);
        }

        KakaoAddressSearchResponse response = kakaoLocalClient.searchAddress(key)
                .doOnSubscribe(s -> log.info("Kakao address conversion start address='{}'", key))
                .doOnError(e -> log.warn("Kakao address conversion fail address='{}'", key, e))
                .onErrorReturn(new KakaoAddressSearchResponse(null, List.of()))
                .block();

        String converted = selectNearestRoadAddress(response, lat, lng);
        addressCache.put(key, converted);

        return converted;
    }

    private String selectNearestRoadAddress(
            KakaoAddressSearchResponse response,
            Double lat,
            Double lng
    ) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return null;
        }

        String fallbackRoadAddress = null;
        Long minDistance = null;
        String nearestRoadAddress = null;

        for (KakaoAddressSearchResponse.Document document : response.documents()) {
            if (document.roadAddress() == null || !hasText(document.roadAddress().addressName())) {
                continue;
            }

            String roadAddress = document.roadAddress().addressName();
            if (fallbackRoadAddress == null) {
                fallbackRoadAddress = roadAddress;
            }

            if (lat == null || lng == null) {
                continue;
            }

            Double documentLng = parseDoubleOrNull(document.roadAddress().x());
            Double documentLat = parseDoubleOrNull(document.roadAddress().y());

            if (documentLng == null || documentLat == null) {
                documentLng = parseDoubleOrNull(document.x());
                documentLat = parseDoubleOrNull(document.y());
            }

            if (documentLng == null || documentLat == null) {
                continue;
            }

            long distance = haversineMeters(lat, lng, documentLat, documentLng);
            if (minDistance == null || distance < minDistance) {
                minDistance = distance;
                nearestRoadAddress = roadAddress;
            }
        }

        return hasText(nearestRoadAddress) ? nearestRoadAddress : fallbackRoadAddress;
    }

    private boolean isLikelyRoadAddress(String address) {
        if (!hasText(address)) {
            return false;
        }

        String normalized = normalizeAddress(address);
        return normalized.contains("로 ")
                || normalized.contains("길 ")
                || normalized.matches(".*(로|길)\\d+번길.*")
                || normalized.matches(".*(로|길)\\s*\\d+.*");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Double parseDoubleOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
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
}