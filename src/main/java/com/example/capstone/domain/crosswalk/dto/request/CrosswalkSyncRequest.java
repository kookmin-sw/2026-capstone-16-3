package com.example.capstone.domain.crosswalk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CrosswalkSyncRequest(
        @NotBlank String ctprvnNm,
        @NotBlank String signguNm
) {
}