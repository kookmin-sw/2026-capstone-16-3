package com.example.capstone.domain.place.dto.response.favorite;

import java.time.LocalDateTime;

// 카테고리 추가 예정
public record FavoritePlaceResponse(
        Long id,
        String placeId,
        String name,
        String alias,
        String address,
        Double lat,
        Double lng,
        LocalDateTime createdAt
) {
}