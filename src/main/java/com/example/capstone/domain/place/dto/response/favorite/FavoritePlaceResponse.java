package com.example.capstone.domain.place.dto.response.favorite;

import java.time.Instant;

public record FavoritePlaceResponse(
        Long id,
        String placeId,
        String name,
        String alias,
        String address,
        Double lat,
        Double lng,
        Instant createdAt
) {
}