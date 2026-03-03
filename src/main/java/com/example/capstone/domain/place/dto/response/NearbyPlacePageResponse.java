package com.example.capstone.domain.place.dto.response;

import java.util.List;

public record NearbyPlacePageResponse(
        List<NearbyPlaceResponse> items,
        int page,
        int size,
        int total,
        boolean hasNext
) {}