package com.example.capstone.domain.place.dto.response;

import java.time.LocalDateTime;

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