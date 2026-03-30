package com.example.capstone.domain.navigation.dto.request;

public record PedestrianRouteRequest(
        String startX,
        String startY,
        String endX,
        String endY,
        String startName,
        String endName
) {
}
