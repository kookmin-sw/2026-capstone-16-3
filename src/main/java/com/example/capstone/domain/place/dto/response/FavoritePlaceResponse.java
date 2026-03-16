package com.example.capstone.domain.place.dto.response;

import java.time.Instant;

public record FavoritePlaceResponse(
        Long id,
        String placeId,
        String name,
        String alias,
        String address,
        Double latitude,
        Double longitude,
        Instant createdAt
) {
}