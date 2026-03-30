package com.example.capstone.domain.navigation.dto.response;

import java.util.List;

public record PedestrianRouteResponse(
        Integer totalDistance,
        Integer totalTime,
        List<RouteStep> steps
) {
}
