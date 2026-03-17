package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.PlaceRecentDeleteAllResponse;
import com.example.capstone.domain.place.dto.response.PlaceRecentDeleteResponse;
import com.example.capstone.domain.place.dto.response.PlaceRecentPageResponse;
import com.example.capstone.domain.place.entity.PlaceRecentSearch;
import com.example.capstone.domain.place.repository.PlaceRecentSearchRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceRecentSearchService {

    private static final int MAX_KEEP = 50;

    private final PlaceRecentSearchRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PlaceRecentPageResponse getRecent(Long userId, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        var result = repository.findByUserIdOrderBySearchedAtDesc(userId, pageable);

        List<PlaceRecentPageResponse.Item> items = result.getContent().stream()
                .map(e -> new PlaceRecentPageResponse.Item(
                        e.getId(),
                        e.getPlaceId(),
                        e.getName(),
                        e.getAddress(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getSearchedAt()
                ))
                .toList();

        return new PlaceRecentPageResponse(
                items,
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public void record(
            Long userId,
            String placeId,
            String name,
            String address,
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

        repository.findByUserIdAndPlaceId(userId, placeId)
                .ifPresentOrElse(
                        existing -> existing.update(
                                name,
                                address,
                                latitude,
                                longitude,
                                now
                        ),
                        () -> repository.save(
                                PlaceRecentSearch.builder()
                                        .user(user)
                                        .placeId(placeId)
                                        .name(name)
                                        .address(address)
                                        .latitude(latitude)
                                        .longitude(longitude)
                                        .searchedAt(now)
                                        .build()
                        )
                );

        trimIfNeeded(userId);
    }

    @Transactional
    public PlaceRecentDeleteResponse deleteOne(Long userId, Long id) {
        boolean deleted = repository.deleteByIdAndUserId(id, userId) > 0;
        return new PlaceRecentDeleteResponse(deleted);
    }

    @Transactional
    public PlaceRecentDeleteAllResponse deleteAll(Long userId) {
        int deletedCount = repository.deleteByUserId(userId);
        return new PlaceRecentDeleteAllResponse(deletedCount);
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