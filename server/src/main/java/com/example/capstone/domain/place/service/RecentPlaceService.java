package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.SliceResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteAllResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceResponse;
import com.example.capstone.domain.place.entity.RecentPlace;
import com.example.capstone.domain.place.repository.RecentPlaceRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentPlaceService {

    private static final int MAX_KEEP = 50;

    private final RecentPlaceRepository repository;
    private final UserRepository userRepository;
    private final KakaoLocalClient kakaoLocalClient;

    @Transactional(readOnly = true)
    public SliceResponse<RecentPlaceResponse> getRecent(Long userId, int page, int size) {
        // PageRequest.of()는 0부터 시작
        var pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(size, 1), MAX_KEEP)
        );

        var result = repository.findByUserIdOrderBySearchedAtDesc(userId, pageable);

        Map<String, String> addressCache = new HashMap<>();

        List<RecentPlaceResponse> items = result.getContent().stream()
                .map(e -> toResponse(e, addressCache))
                .toList();

        return SliceResponse.of(items, page, size, result.hasNext());
    }

    @Transactional
    public void record(
            Long userId,
            String placeId,
            String name,
            String roadAddress,
            String jibunAddress,
            Double latitude,
            Double longitude
    ) {
        if (userId == null) return;
        if (placeId == null || placeId.isBlank()) return;
        if (name == null || name.isBlank()) return;
        if (latitude == null || longitude == null) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Instant now = Instant.now();

        String normalizedRoadAddress = resolveRoadAddress(
                roadAddress,
                jibunAddress,
                latitude,
                longitude,
                new HashMap<>()
        );

        String normalizedJibunAddress = normalizeAddress(jibunAddress);

        repository.findByUserIdAndPlaceId(userId, placeId)
                .ifPresentOrElse(
                        existing -> existing.update(
                                name,
                                normalizedRoadAddress,
                                normalizedJibunAddress,
                                latitude,
                                longitude,
                                now
                        ),
                        () -> repository.save(
                                RecentPlace.builder()
                                        .user(user)
                                        .placeId(placeId)
                                        .name(name)
                                        .roadAddress(normalizedRoadAddress)
                                        .jibunAddress(normalizedJibunAddress)
                                        .latitude(latitude)
                                        .longitude(longitude)
                                        .searchedAt(now)
                                        .build()
                        )
                );

        trimIfNeeded(userId);
    }

    @Transactional
    public RecentPlaceDeleteResponse deleteOne(Long userId, Long id) {
        boolean deleted = repository.deleteByIdAndUserId(id, userId) > 0;
        return new RecentPlaceDeleteResponse(deleted);
    }

    @Transactional
    public RecentPlaceDeleteAllResponse deleteAll(Long userId) {
        int deletedCount = repository.deleteByUserId(userId);
        return new RecentPlaceDeleteAllResponse(deletedCount);
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

    private String selectNearestRoadAddress(
            KakaoAddressSearchResponse resp,
            Double latitude,
            Double longitude
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

            String roadAddress = doc.roadAddress().addressName();
            if (fallbackRoadAddress == null) {
                fallbackRoadAddress = roadAddress;
            }

            if (latitude == null || longitude == null) {
                continue;
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

            long distance = haversineMeters(latitude, longitude, docLat, docLng);
            if (minDistance == null || distance < minDistance) {
                minDistance = distance;
                nearestRoadAddress = roadAddress;
            }
        }

        return hasText(nearestRoadAddress) ? nearestRoadAddress : fallbackRoadAddress;
    }

    private String convertJibunToRoadAddress(
            String address,
            Double latitude,
            Double longitude,
            Map<String, String> addressCache
    ) {
        if (!hasText(address)) {
            return null;
        }

        String key = normalizeAddress(address);
        if (addressCache.containsKey(key)) {
            return addressCache.get(key);
        }

        KakaoAddressSearchResponse resp = kakaoLocalClient.searchAddress(key)
                .doOnSubscribe(s -> log.info("Kakao recent place address conversion start address='{}'", key))
                .doOnError(e -> log.warn("Kakao recent place address conversion fail address='{}'", key, e))
                .onErrorReturn(new KakaoAddressSearchResponse(null, List.of()))
                .block();

        String converted = selectNearestRoadAddress(resp, latitude, longitude);
        addressCache.put(key, converted);
        return converted;
    }

    private String resolveRoadAddress(
            String roadAddress,
            String jibunAddress,
            Double latitude,
            Double longitude,
            Map<String, String> addressCache
    ) {
        if (isLikelyRoadAddress(roadAddress)) {
            return normalizeAddress(roadAddress);
        }

        if (hasText(jibunAddress)) {
            String converted = convertJibunToRoadAddress(jibunAddress, latitude, longitude, addressCache);
            if (hasText(converted)) {
                return normalizeAddress(converted);
            }
            return normalizeAddress(jibunAddress);
        }

        if (hasText(roadAddress)) {
            String converted = convertJibunToRoadAddress(roadAddress, latitude, longitude, addressCache);
            if (hasText(converted)) {
                return normalizeAddress(converted);
            }
            return normalizeAddress(roadAddress);
        }

        return null;
    }
    
    private RecentPlaceResponse toResponse(RecentPlace recentPlace, Map<String, String> addressCache) {
        String roadAddress = resolveRoadAddress(
                recentPlace.getRoadAddress(),
                recentPlace.getJibunAddress(),
                recentPlace.getLatitude(),
                recentPlace.getLongitude(),
                addressCache
        );

        return new RecentPlaceResponse(
                recentPlace.getId(),
                recentPlace.getPlaceId(),
                recentPlace.getName(),
                roadAddress,
                recentPlace.getLatitude(),
                recentPlace.getLongitude(),
                recentPlace.getSearchedAt()
        );
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

    private void trimIfNeeded(Long userId) {
        long count = repository.countByUserId(userId);
        if (count <= MAX_KEEP) {
            return;
        }

        var overflow = repository.findByUserIdOrderBySearchedAtDesc(userId, PageRequest.of(MAX_KEEP, Integer.MAX_VALUE))
                .getContent();

        if (!overflow.isEmpty()) {
            repository.deleteAll(overflow);
        }
    }
}