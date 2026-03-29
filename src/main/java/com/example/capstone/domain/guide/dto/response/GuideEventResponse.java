package com.example.capstone.domain.guide.dto.response;

public record GuideEventResponse(
        String status,
        String guideText,
        String primaryObjectClass,
        String clockDirection,
        String distance,
        String alertLevel
) {
}
