package com.example.capstone.domain.place.dto.response.recent;

import java.time.Instant;

public record RecentPlaceResponse(
        Long id,
        String placeId,
        String name,
        String address,
        Double lat,
        Double lng,
        Instant searchedAt
) {}