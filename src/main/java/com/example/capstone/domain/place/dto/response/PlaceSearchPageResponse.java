package com.example.capstone.domain.place.dto.response;

import java.util.List;

/**
 * GET /api/places/search 응답
 */
public record PlaceSearchPageResponse(
        List<Item> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public record Item(
            String placeId,
            String name,
            String category,
            String address,
            String roadAddress,
            double latitude,
            double longitude,
            Long distanceMeters,
            String phone,
            String placeUrl,
            String provider
    ) {}
}