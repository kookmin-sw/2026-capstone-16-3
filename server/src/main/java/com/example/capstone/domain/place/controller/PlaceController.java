package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.*;
import com.example.capstone.domain.place.service.RecentPlaceService;
import com.example.capstone.domain.place.service.PlaceService;
import com.example.capstone.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@Tag(name = "[장소]")
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;
    private final RecentPlaceService recentPlaceService;

    public PlaceController(
            PlaceService placeService,
            RecentPlaceService recentPlaceService
    ) {
        this.placeService = placeService;
        this.recentPlaceService = recentPlaceService;
    }

    @GetMapping("/nearby")
    public ApiResponse<SliceResponse<PlaceResponse>> nearby(
            @Parameter(description = "카테고리 코드")
            @RequestParam String code,
            @Parameter(description = "위도")
            @RequestParam double lat,
            @Parameter(description = "경도")
            @RequestParam double lng,
            @Parameter(description = "반경(m)")
            @RequestParam(defaultValue = "3000") @Min(0) @Max(20000) int radius,
            @RequestParam(defaultValue = "1") @Min(1) @Max(45) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size
    ) {
        SliceResponse<PlaceResponse> result = placeService.searchNearbyByCategory(
                code, lat, lng, radius, page, size
        );
        return ApiResponse.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<SliceResponse<PlaceResponse>> search(
            @Parameter(description = "검색어")
            @RequestParam String query,
            @Parameter(description = "위도")
            @RequestParam double lat,
            @Parameter(description = "경도")
            @RequestParam double lng,
            @Parameter(description = "반경(m)")
            @RequestParam(defaultValue = "3000") @Min(0) @Max(20000) int radius,
            @RequestParam(defaultValue = "1") @Min(1) @Max(45) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size
    ) {
        SliceResponse<PlaceResponse> result = placeService.searchPlaces(
                query, lat, lng, radius, page, size
        );
        return ApiResponse.ok(result);
    }

    /**
     * 장소 상세 조회
     * - 카카오 REST API에서는 상세 조회가 없음
     * -> 캐시 기반 조회(검색/nearby로 한번이라도 조회된 항목)
     */
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailResponse> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String placeId
    ) {
        PlaceDetailResponse result = placeService.getPlaceDetail(placeId);

        if (userId != null) {
            recentPlaceService.record(
                    userId,
                    result.placeId(),
                    result.name(),
                    result.roadAddress(),
                    result.address(),
                    result.lat(),
                    result.lng()
            );
        }

        return ApiResponse.ok(result);
    }

    @GetMapping("/geocode")
    public ApiResponse<GeocodeResponse> geocode(
            @Parameter(description = "주소")
            @RequestParam String address
    ) {
        return ApiResponse.ok(placeService.geocode(address));
    }

    @GetMapping("/reverse-geocode")
    public ApiResponse<ReverseGeocodeResponse> reverseGeocode(
            @Parameter(description = "위도")
            @RequestParam double lat,
            @Parameter(description = "경도")
            @RequestParam double lng
    ) {
        return ApiResponse.ok(placeService.reverseGeocode(lat, lng));
    }
}