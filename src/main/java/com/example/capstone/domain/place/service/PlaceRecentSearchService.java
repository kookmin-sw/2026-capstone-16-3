package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.PlaceRecentPageResponse;
import com.example.capstone.domain.place.entity.PlaceRecentSearch;
import com.example.capstone.domain.place.repository.PlaceRecentSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PlaceRecentSearchService {

    private static final int MAX_KEEP = 50;

    private final PlaceRecentSearchRepository repository;

    public PlaceRecentSearchService(PlaceRecentSearchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlaceRecentPageResponse getRecent(String userKey, int page, int size) {
        String normalizedUserKey = normalizeUserKey(userKey);

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        var p = repository.findByUserKeyOrderBySearchedAtDesc(normalizedUserKey, pageable);

        List<PlaceRecentPageResponse.Item> items = p.getContent().stream()
                .map(e -> new PlaceRecentPageResponse.Item(
                        e.getId(),
                        e.getPlaceId(),
                        e.getName(),
                        e.getCategory(),
                        e.getDistanceM(),
                        e.getDirectionClock(),
                        e.getRoadAddress(),
                        e.getSearchedAt()
                ))
                .toList();

        return new PlaceRecentPageResponse(
                items,
                page,
                size,
                p.getTotalElements(),
                p.getTotalPages()
        );
    }

    @Transactional
    public void record(
            String userKey,
            String placeId,
            String name,
            String category,
            Long distanceM,
            Integer directionClock,
            String roadAddress,
            String jibunAddress
    ) {
        String normalizedUserKey = normalizeUserKey(userKey);

        if (placeId == null || placeId.isBlank()) return;
        if (name == null || name.isBlank()) return;

        Instant now = Instant.now();

        repository.findByUserKeyAndPlaceId(normalizedUserKey, placeId)
                .ifPresentOrElse(
                        existing -> existing.touch(
                                distanceM,
                                directionClock,
                                roadAddress,
                                jibunAddress,
                                now
                        ),
                        () -> repository.save(new PlaceRecentSearch(
                                normalizedUserKey,
                                placeId,
                                name,
                                category,
                                distanceM,
                                directionClock,
                                roadAddress,
                                jibunAddress,
                                now
                        ))
                );

        // 아직 실제 정리 로직이 없으면 이 블록은 지우는 편이 낫다.
        // long count = repository.countByUserKey(normalizedUserKey);
        // if (count > MAX_KEEP) { ... }
    }

    @Transactional
    public boolean deleteOne(String userKey, Long id) {
        if (id == null) return false;
        String normalizedUserKey = normalizeUserKey(userKey);
        return repository.deleteByIdAndUserKey(id, normalizedUserKey) > 0;
    }

    @Transactional
    public int deleteAll(String userKey) {
        String normalizedUserKey = normalizeUserKey(userKey);
        return repository.deleteByUserKey(normalizedUserKey);
    }

    private String normalizeUserKey(String userKey) {
        return (userKey == null || userKey.isBlank()) ? "anonymous" : userKey;
    }
}