package com.example.capstone.domain.place.dto.response.recent;

import java.time.Instant;
import java.util.List;

public record RecentPlacePageResponse(
        List<Item> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public record Item(
            Long id,
            String placeId,
            String name,
            String address,
            Double lat,
            Double lng,
            Instant searchedAt
    ) {}
}