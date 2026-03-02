package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.NearbyPlaceResponse;
import com.example.capstone.domain.place.service.PlaceService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * 예)
     * /api/places/nearby?lat=37.5665&lng=126.9780&radius=500&codes=CS2,SW8,PM9
     */
    @GetMapping("/nearby")
    public ApiResponse<List<NearbyPlaceResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") @Min(1) @Max(3000) int radius,
            @RequestParam String codes,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int sizePerCategory,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int maxItems
    ) {
        List<String> categoryCodes = Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        List<NearbyPlaceResponse> result = placeService.findNearbyByCategoryCodes(
                lat, lng, radius, categoryCodes, sizePerCategory, maxItems
        );

        // ApiResponse는 프로젝트 구현에 따라 메서드명이 다를 수 있음
        // 예: ApiResponse.success(result) / ApiResponse.ok(result) 등으로 맞춰서 수정
        return ApiResponse.ok(result);
    }
}