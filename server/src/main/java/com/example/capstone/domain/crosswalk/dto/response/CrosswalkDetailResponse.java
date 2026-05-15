package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkDetailResponse(
        String crosswalkId,
        CrosswalkLocationDto location,
        CrosswalkInfoDto crosswalk,
        AcousticSignalDto acousticSignal,
        CrosswalkGuidanceSummaryDto guidance,
        CrosswalkSourceDto source
) {
}