package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkNearbyResponse(
        String crosswalkId,
        Double distanceMeters,
        CrosswalkLocationDto location,
        CrosswalkInfoSummaryDto crosswalk,
        AcousticSignalSummaryDto acousticSignal,
        CrosswalkGuidanceSummaryDto guidance
) {
}