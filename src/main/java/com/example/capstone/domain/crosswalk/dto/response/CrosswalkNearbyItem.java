package com.example.capstone.domain.crosswalk.dto.response;

import java.util.List;

public record CrosswalkNearbyItem(
        String crosswalkManageNo,
        String roadName,
        String address,
        Double latitude,
        Double longitude,
        Double distanceMeters,
        Integer riskScore,
        List<String> guidanceMessages
) {
}