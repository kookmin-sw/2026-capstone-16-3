package com.example.capstone.domain.crosswalk.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrosswalkCandidateRequest(
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @NotNull
        @DecimalMin(value = "10.0")
        @DecimalMax(value = "300.0")
        Double radiusMeters,

        @NotNull
        @Min(1)
        @Max(10)
        Integer maxCandidates
) {
}
