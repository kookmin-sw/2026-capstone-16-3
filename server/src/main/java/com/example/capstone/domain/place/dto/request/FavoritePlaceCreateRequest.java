package com.example.capstone.domain.place.dto.request;

import jakarta.validation.constraints.*;

public record FavoritePlaceCreateRequest(
        @NotBlank
        @Size(max = 128)
        String placeId,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String alias,

        @Size(max = 500)
        String address,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double lat,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double lng
) {
}