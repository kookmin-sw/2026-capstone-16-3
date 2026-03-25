package com.example.capstone.domain.crosswalk.controller;

import com.example.capstone.domain.crosswalk.dto.request.CrosswalkSyncRequest;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import com.example.capstone.domain.crosswalk.service.CrosswalkQueryService;
import com.example.capstone.domain.crosswalk.service.CrosswalkSyncService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/crosswalks")
public class CrosswalkController {

    private final CrosswalkSyncService crosswalkSyncService;
    private final CrosswalkQueryService crosswalkQueryService;

    @PostMapping("/sync")
    public ApiResponse<Void> sync(@Valid @RequestBody CrosswalkSyncRequest request) {
        crosswalkSyncService.syncByRegion(request.ctprvnNm(), request.signguNm());
        return ApiResponse.ok();
    }

    @GetMapping("/nearby")
    public ApiResponse<CrosswalkNearbyResponse> getNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "80") double radiusMeters,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.ok(
                crosswalkQueryService.findNearby(latitude, longitude, radiusMeters, limit)
        );
    }
}