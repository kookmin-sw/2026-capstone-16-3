package com.example.capstone.domain.place.dto.response.favorite;

import java.util.List;

public record FavoritePlacePageResponse(
        List<FavoritePlaceResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}