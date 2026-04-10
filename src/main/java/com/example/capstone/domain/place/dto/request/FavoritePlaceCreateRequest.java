package com.example.capstone.domain.place.dto.request;

import com.example.capstone.domain.place.entity.FavoritePlaceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record FavoritePlaceCreateRequest(
        @NotBlank
        @Size(max = 128)
        @Schema(description = "카카오 장소 ID")
        String placeId,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "장소명")
        String name,

        @Size(max = 255)
        @Schema(description = "별칭")
        String alias,

        @Size(max = 500)
        @Schema(description = "도로명주소")
        String address,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        @Schema(description = "위도")
        Double lat,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Schema(description = "경도")
        Double lng,

        @NotNull
        @Schema(description = "카테고리")
        FavoritePlaceCategory category
) {
}