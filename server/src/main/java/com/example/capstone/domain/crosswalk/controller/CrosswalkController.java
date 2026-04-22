package com.example.capstone.domain.crosswalk.controller;

import com.example.capstone.domain.crosswalk.dto.response.CrosswalkDetailResponse;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import com.example.capstone.domain.crosswalk.service.CrosswalkQueryService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crosswalks")
@RequiredArgsConstructor
public class CrosswalkController {

    private final CrosswalkQueryService crosswalkQueryService;

    @GetMapping("/nearby")
    public ApiResponse<List<CrosswalkNearbyResponse>> getNearbyCrosswalks(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "50") double radiusMeters
    ) {
        return ApiResponse.ok(
                crosswalkQueryService.getNearbyCrosswalks(latitude, longitude, radiusMeters)
        );
    }

    @GetMapping("/{crosswalkCode}")
    public ApiResponse<CrosswalkDetailResponse> getCrosswalkDetail(
            @PathVariable String crosswalkCode
    ) {
        return ApiResponse.ok(
                crosswalkQueryService.getCrosswalkDetail(crosswalkCode)
        );
    }
}