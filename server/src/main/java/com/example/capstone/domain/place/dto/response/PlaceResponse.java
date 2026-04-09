package com.example.capstone.domain.place.dto.response;

public record PlaceResponse(
        String placeId,
        String name,
        String category,
        String roadAddress,
        double lat,
        double lng,
        Long distanceM,
        Integer directionClock
) {}

