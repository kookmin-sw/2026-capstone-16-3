package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkInfoSummaryDto(
        Boolean pedestrianSignal,
        Boolean brailleBlock,
        Boolean curbLowered
) {
}