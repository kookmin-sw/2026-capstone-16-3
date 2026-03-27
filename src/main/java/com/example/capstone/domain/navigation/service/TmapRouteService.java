package com.example.capstone.domain.navigation.service;

import com.example.capstone.domain.navigation.dto.request.PedestrianRouteRequest;
import com.example.capstone.domain.navigation.dto.response.PedestrianRouteResponse;
import com.example.capstone.domain.navigation.parser.TmapRouteResponseParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class TmapRouteService {

    private final WebClient tmapWebClient;
    private final TmapRouteResponseParser tmapRouteResponseParser;

    public TmapRouteService(
            @Qualifier("tmapWebClient") WebClient tmapWebClient,
            TmapRouteResponseParser tmapRouteResponseParser
    ) {
        this.tmapWebClient = tmapWebClient;
        this.tmapRouteResponseParser = tmapRouteResponseParser;
    }

    @Value("${sk.tmap.app-key}")
    private String appKey;

    public PedestrianRouteResponse getPedestrianRoute(PedestrianRouteRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("startX", request.startX());
        body.put("startY", request.startY());
        body.put("endX", request.endX());
        body.put("endY", request.endY());
        body.put("startName", request.startName());
        body.put("endName", request.endName());

        String responseBody = tmapWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("version", "1")
                        .build())
                .header("appKey", appKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(error -> new IllegalStateException("TMAP API 호출 실패: " + error))
                )
                .bodyToMono(String.class)
                .block();

        return tmapRouteResponseParser.parse(responseBody);
    }
}
