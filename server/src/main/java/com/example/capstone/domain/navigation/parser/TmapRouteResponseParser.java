package com.example.capstone.domain.navigation.parser;

import com.example.capstone.domain.navigation.dto.response.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TmapRouteResponseParser {

    private final ObjectMapper objectMapper;

    public PedestrianRouteResponse parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode features = root.path("features");

            List<RouteStep> steps = new ArrayList<>();

            Integer totalDistance = null;
            Integer totalTime = null;

            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                JsonNode properties = feature.path("properties");

                String type = geometry.path("type").asText();

                // 전체 거리/시간 (첫 Point에서만 존재)
                if (totalDistance == null && properties.has("totalDistance")) {
                    totalDistance = properties.path("totalDistance").asInt();
                }

                if (totalTime == null && properties.has("totalTime")) {
                    totalTime = properties.path("totalTime").asInt();
                }

                // Point 처리
                if ("Point".equals(type)) {
                    JsonNode coordinates = geometry.path("coordinates");

                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();

                    steps.add(new RoutePointStep(
                            "POINT",
                            lat,
                            lon,
                            properties.path("description").asText(),
                            properties.has("turnType") ? properties.path("turnType").asInt() : null,
                            properties.path("pointType").asText(),
                            properties.path("facilityType").asText()
                    ));
                }

                // LineString 처리
                if ("LineString".equals(type)) {
                    JsonNode coordinates = geometry.path("coordinates");

                    List<RoutePoint> path = new ArrayList<>();

                    for (JsonNode coord : coordinates) {
                        double lon = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        path.add(new RoutePoint(lat, lon));
                    }

                    steps.add(new RouteLineStep(
                            "LINE",
                            path,
                            properties.has("distance") ? properties.path("distance").asInt() : null,
                            properties.has("time") ? properties.path("time").asInt() : null,
                            properties.path("facilityType").asText()
                    ));
                }
            }

            return new PedestrianRouteResponse(
                    totalDistance,
                    totalTime,
                    steps
            );

        } catch (Exception e) {
            throw new IllegalStateException("TMAP 응답 파싱 실패", e);
        }
    }
}
