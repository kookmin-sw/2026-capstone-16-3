package com.example.capstone.domain.crosswalk.dto.response;

import java.util.List;

public record CrosswalkCandidateResponse(
        String requestId,
        UserLocation userLocation,
        double searchRadiusMeters,
        List<CrosswalkCandidateItem> candidates
) {
    public record UserLocation(
            double latitude,
            double longitude
    ) {
    }
}
