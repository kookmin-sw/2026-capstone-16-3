package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.request.FavoritePlaceCreateRequest;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlaceCreateResponse;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlacePageResponse;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlaceResponse;
import com.example.capstone.domain.place.entity.FavoritePlace;
import com.example.capstone.domain.place.repository.FavoritePlaceRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritePlaceService {

    // 즐겨찾기 최대 저장 개수
    private static final int MAX_FAVORITES = 50;

    private final FavoritePlaceRepository favoritePlaceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public FavoritePlacePageResponse getFavorites(Long userId, int page, int size) {
        User user = getUser(userId);

        // PageRequest.of()는 0부터 시작
        var pageable = PageRequest.of(
                Math.max(page, 1) - 1, // page=0이 첫 페이지
                Math.min(Math.max(size, 1), MAX_FAVORITES)
        );

        var result = favoritePlaceRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<FavoritePlaceResponse> items = result.getContent().stream()
                .map(f -> new FavoritePlaceResponse(
                        f.getId(),
                        f.getPlaceId(),
                        f.getName(),
                        f.getAlias(),
                        f.getAddress(),
                        f.getLatitude(),
                        f.getLongitude(),
                        f.getCategory(),
                        f.getCreatedAt()
                ))
                .toList();

        return new FavoritePlacePageResponse(
                items,
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public FavoritePlaceCreateResponse createFavorite(Long userId, FavoritePlaceCreateRequest request) {
        User user = getUser(userId);

        if (favoritePlaceRepository.existsByUserIdAndPlaceId(user.getId(), request.placeId())) {
            throw new BusinessException("FAVORITE_ALREADY_EXISTS", "이미 즐겨찾기에 등록된 장소입니다.");
        }

        long count = favoritePlaceRepository.countByUserId(user.getId());
        if (count >= MAX_FAVORITES) {
            throw new BusinessException("FAVORITE_LIMIT_EXCEEDED", "즐겨찾기는 최대 50개까지 등록할 수 있습니다.");
        }

        FavoritePlace favoritePlace = FavoritePlace.builder()
                .user(user)
                .placeId(request.placeId())
                .name(request.name())
                .alias(request.alias())
                .address(request.address())
                .latitude(request.lat())
                .longitude(request.lng())
                .category(request.category())
                .build();

        FavoritePlace saved = favoritePlaceRepository.save(favoritePlace);

        return new FavoritePlaceCreateResponse(
                true,
                saved.getId(),
                saved.getCategory()
        );
    }

    @Transactional
    public FavoritePlaceDeleteResponse deleteFavorite(Long userId, Long id) {
        getUser(userId);

        boolean deleted = favoritePlaceRepository.deleteByIdAndUserId(id, userId) > 0;

        if (!deleted) {
            throw new BusinessException("FAVORITE_NOT_FOUND", "해당 즐겨찾기를 찾을 수 없습니다.");
        }

        return new FavoritePlaceDeleteResponse(true);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }
}