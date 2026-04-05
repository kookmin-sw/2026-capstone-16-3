package com.example.capstone.domain.crosswalk.controller;

import com.example.capstone.domain.crosswalk.dto.request.CrosswalkCandidateRequest;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateResponse;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import com.example.capstone.domain.crosswalk.service.CrosswalkCandidateService;
import com.example.capstone.domain.crosswalk.service.CrosswalkPublicApiService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequiredArgsConstructor
@Validated
public class CrosswalkController {

    private final CrosswalkCandidateService crosswalkCandidateService;
    private final CrosswalkPublicApiService crosswalkPublicApiService;

    @PostMapping("/api/v1/crosswalks/candidates")
    public ApiResponse<CrosswalkCandidateResponse> findCandidates(
            @Valid @RequestBody CrosswalkCandidateRequest request
    ) {
        return ApiResponse.ok(crosswalkCandidateService.findCandidates(request));
    }

    @GetMapping("/api/crosswalks/nearby")
    public ApiResponse<CrosswalkNearbyResponse> getNearby(
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") double latitude,
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") double longitude,
            @RequestParam(defaultValue = "50") @Min(10) @Max(100) double radiusMeters,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit
    ) {
        CrosswalkCandidateRequest request = new CrosswalkCandidateRequest(latitude, longitude, radiusMeters, limit);
        return ApiResponse.ok(crosswalkCandidateService.findNearbyResponse(request));
    }

    @GetMapping("/api/crosswalks/raw")
    public String getRawResponse(
            @RequestParam(defaultValue = "서울특별시") String ctprvnNm,
            @RequestParam(defaultValue = "동작구") String signguNm
    ) {
        return crosswalkPublicApiService.fetchTest(
                ctprvnNm,
                signguNm
        );
    }
}
