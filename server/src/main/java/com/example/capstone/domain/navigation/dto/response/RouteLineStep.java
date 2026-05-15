package com.example.capstone.domain.navigation.dto.response;

import java.util.List;

public record RouteLineStep(
        String type,
        List<RoutePoint> path,
        Integer distance,
        Integer time,
        String facilityType
) implements RouteStep {
}
