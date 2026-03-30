package com.example.capstone.domain.navigation.controller;

import com.example.capstone.domain.navigation.dto.request.PedestrianRouteRequest;
import com.example.capstone.domain.navigation.dto.response.PedestrianRouteResponse;
import com.example.capstone.domain.navigation.service.TmapRouteService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/navigation")
public class NavigationController {

    private final TmapRouteService tmapRouteService;

    @PostMapping("/routes")
    public ApiResponse<PedestrianRouteResponse> getPedestrianRoute(
            @RequestBody PedestrianRouteRequest request
    ) {
        return ApiResponse.ok(tmapRouteService.getPedestrianRoute(request));
    }
}
