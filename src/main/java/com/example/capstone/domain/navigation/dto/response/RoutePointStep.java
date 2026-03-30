package com.example.capstone.domain.navigation.dto.response;

public record RoutePointStep(
        String type,
        Double latitude,
        Double longitude,
        String description,
        Integer turnType,
        String pointType
) implements RouteStep {
}
