package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.SliceResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteAllResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceResponse;
import com.example.capstone.domain.place.entity.RecentPlace;
import com.example.capstone.domain.place.repository.RecentPlaceRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
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
    private final PlaceAddressResolver placeAddressResolver;

    @Transactional(readOnly = true)
    public SliceResponse<RecentPlaceResponse> getRecent(Long userId, int page, int size) {
        // PageRequest.of()는 0부터 시작

        int requestPage = Math.max(page, 1);
        int requestSize = Math.min(Math.max(size, 1), MAX_KEEP);

        var pageable = PageRequest.of(requestPage - 1, requestSize);

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
            Double lat,
            Double lng
    ) {
        if (userId == null) return;
        if (placeId == null || placeId.isBlank()) return;
        if (name == null || name.isBlank()) return;
        if (lat == null || lng == null) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Instant now = Instant.now();

        String normalizedRoadAddress = placeAddressResolver.resolveRoadAddress(
                jibunAddress,
                roadAddress,
                lat,
                lng,
                new HashMap<>()
        );

        String normalizedJibunAddress = placeAddressResolver.normalizeAddress(jibunAddress);

        repository.findByUserIdAndPlaceId(userId, placeId)
                .ifPresentOrElse(
                        existing -> existing.update(
                                name,
                                normalizedRoadAddress,
                                normalizedJibunAddress,
                                lat,
                                lng,
                                now
                        ),
                        () -> repository.save(
                                RecentPlace.builder()
                                        .user(user)
                                        .placeId(placeId)
                                        .name(name)
                                        .roadAddress(normalizedRoadAddress)
                                        .jibunAddress(normalizedJibunAddress)
                                        .latitude(lat)
                                        .longitude(lng)
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

    private RecentPlaceResponse toResponse(RecentPlace recentPlace, Map<String, String> addressCache) {
        String roadAddress = placeAddressResolver.resolveRoadAddress(
                recentPlace.getJibunAddress(),
                recentPlace.getRoadAddress(),
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

    private void trimIfNeeded(Long userId) {
        List<RecentPlace> all = repository.findAllByUserIdOrderBySearchedAtDesc(userId);

        if (all.size() <= MAX_KEEP) {
            return;
        }

        List<RecentPlace> overflow = all.subList(MAX_KEEP, all.size());
        repository.deleteAll(overflow);
    }
}