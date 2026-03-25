package com.example.capstone.domain.crosswalk.dto.response;

import java.util.List;

public record CrosswalkNearbyResponse(
        List<CrosswalkNearbyItem> items,
        int totalItems
) {
}