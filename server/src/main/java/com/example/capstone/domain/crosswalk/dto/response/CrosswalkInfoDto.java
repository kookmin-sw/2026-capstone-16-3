package com.example.capstone.domain.crosswalk.dto.response;

public record CrosswalkInfoDto(
        String kind,
        Double width,
        Double length,
        Boolean pedestrianSignal,
        Boolean actuatedSignal,
        Integer greenTime,
        Integer redTime,
        Boolean brailleBlock,
        Boolean curbLowered,
        Boolean trafficIsland,
        Boolean safetyLighting
) {
}