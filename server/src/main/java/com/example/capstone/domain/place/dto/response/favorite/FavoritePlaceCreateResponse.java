package com.example.capstone.domain.place.dto.response.favorite;

import com.example.capstone.domain.place.entity.FavoritePlaceCategory;

public record FavoritePlaceCreateResponse(
        boolean created,
        Long id,
        FavoritePlaceCategory category
) {
}