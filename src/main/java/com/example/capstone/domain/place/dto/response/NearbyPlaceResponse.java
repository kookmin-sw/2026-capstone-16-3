package com.example.capstone.domain.place.dto.response;

import java.util.List;


public record NearbyPlaceResponse(
        String id,
        String name,
        double lat,
        double lng,
        Long distanceM,
        String category,
        String roadAddress,
        String jibunAddress,
        String phone,
        String placeUrl,
        String provider
) {}

