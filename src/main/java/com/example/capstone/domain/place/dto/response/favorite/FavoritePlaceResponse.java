package com.example.capstone.domain.place.dto.response.favorite;

import com.example.capstone.domain.place.entity.FavoritePlaceCategory;

import java.time.LocalDateTime;

public record FavoritePlaceResponse(
        Long id,
        String placeId,
        String name,
        String alias,
        String address,
        Double lat,
        Double lng,
        FavoritePlaceCategory category,
        LocalDateTime createdAt
) {
}