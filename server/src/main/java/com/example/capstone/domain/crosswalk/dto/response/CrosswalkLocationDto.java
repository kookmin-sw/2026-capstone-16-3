package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkLocationDto(
        Double latitude,
        Double longitude,
        String roadAddress,
        String sido,
        String sigungu,
        String emd
) {
}