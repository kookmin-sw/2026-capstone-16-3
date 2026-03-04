package com.example.capstone.domain.place.dto.response;

public record NearbyPlaceResponse(
        String id,
        String name,
        double lat,
        double lng,
        Long distanceM,
        Integer directionClock,
        String category,
        String roadAddress,
        String jibunAddress,
        String phone,
        Boolean openNow,
        String placeUrl,
        String provider
) {}

