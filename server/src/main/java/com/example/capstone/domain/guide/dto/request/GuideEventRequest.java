package com.example.capstone.domain.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GuideEventRequest(
        @JsonProperty("user_id")
        String userId,

        String status,

        @JsonProperty("processed_at")
        String processedAt,

        @JsonProperty("processing_time_ms")
        Integer processingTimeMs,

        @JsonProperty("guide_text")
        String guideText,

        @JsonProperty("primary_object_class")
        String primaryObjectClass,

        @JsonProperty("clock_direction")
        String clockDirection,

        String distance,

        @JsonProperty("alert_level")
        String alertLevel,

        @JsonProperty("primary_object_id")
        String primaryObjectId
) {
}
