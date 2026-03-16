package com.example.capstone.domain.place.dto.response;

import java.util.List;

/**
 * GET /api/places/{placeId} 응답
 */
public record PlaceDetailResponse(
        String placeId,
        String name,
        String category,
        String address,
        String roadAddress,
        double latitude,
        double longitude,
        Long distanceFromCenterM,
        String phone,
        String placeUrl,
        OpeningHours openingHours,
        Extra extra
) {
    public record OpeningHours(
            boolean is24h,
            List<DayHours> weekly
    ) {
        public record DayHours(
                String day,
                String open,
                String close,
                boolean closed
        ) {}
    }

    public record Extra(
            String provider
    ) {}
}