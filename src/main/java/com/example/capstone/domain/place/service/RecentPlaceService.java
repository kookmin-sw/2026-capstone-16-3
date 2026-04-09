package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteAllResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.recent.RecentPlacePageResponse;
import com.example.capstone.domain.place.entity.RecentPlace;
import com.example.capstone.domain.place.repository.RecentPlaceRepository;
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
public class RecentPlaceService {

    private static final int MAX_KEEP = 50;

    private final RecentPlaceRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RecentPlacePageResponse getRecent(Long userId, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 1),
                Math.min(Math.max(size, 1), MAX_KEEP)
        );

        var result = repository.findByUserIdOrderBySearchedAtDesc(userId, pageable);

        List<RecentPlacePageResponse.Item> items = result.getContent().stream()
                .map(e -> new RecentPlacePageResponse.Item(
                        e.getId(),
                        e.getPlaceId(),
                        e.getName(),
                        e.getAddress(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getSearchedAt()
                ))
                .toList();

        return new RecentPlacePageResponse(
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
                                RecentPlace.builder()
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
    public RecentPlaceDeleteResponse deleteOne(Long userId, Long id) {
        boolean deleted = repository.deleteByIdAndUserId(id, userId) > 0;
        return new RecentPlaceDeleteResponse(deleted);
    }

    @Transactional
    public RecentPlaceDeleteAllResponse deleteAll(Long userId) {
        int deletedCount = repository.deleteByUserId(userId);
        return new RecentPlaceDeleteAllResponse(deletedCount);
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