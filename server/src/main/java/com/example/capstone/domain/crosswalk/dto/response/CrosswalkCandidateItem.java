package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkCandidateItem(
        String candidateId,
        double distanceMeters,
        double latitude,
        double longitude,
        String roadName,
        boolean pedestrianSignal,
        boolean audioSignal,
        boolean brailleBlock,
        boolean curbLowered,
        String source
) {
}
