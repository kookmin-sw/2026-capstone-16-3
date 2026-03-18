package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.PlacePageResponse;
import com.example.capstone.domain.place.dto.response.PlaceDetailResponse;
import com.example.capstone.domain.place.dto.response.PlaceResponse;
import com.example.capstone.domain.place.service.RecentPlaceService;
import com.example.capstone.domain.place.service.PlaceService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RestController
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

    /**
     * 예)
     * /api/places/nearby?lat=37.5665&lng=126.9780&radius=3000&code=CS2&page=1&size=30
     * - 카테고리는 한 번에 하나만 조회한다.
     * - 호환을 위해 codes 파라미터도 받되, 콤마로 여러 개가 오면 400 처리한다.
     */
    @GetMapping("/nearby")
    public ApiResponse<PlacePageResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") @Min(0) @Max(3000) int radius,

            @RequestParam(required = false) String code,
            @RequestParam(required = false) String codes,

            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int sizePerCategory,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size
    ) {
        String raw = (code != null && !code.isBlank()) ? code : codes;
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("'code' is required. ex) code=CS2");
        }

        List<String> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (parsed.size() != 1) {
            throw new IllegalArgumentException("Only one category code is allowed. ex) code=CS2");
        }

        // 서비스는 전체 정렬 리스트 생성만 책임
        List<PlaceResponse> all = placeService.findNearbyByCategoryCodes(
                lat, lng, radius, parsed, sizePerCategory, Integer.MAX_VALUE
        );

        int total = all.size();
        int from = (page - 1) * size;

        if (from >= total) {
            return ApiResponse.ok(new PlacePageResponse(List.of(), page, size, total, false));
        }

        int to = Math.min(from + size, total);
        List<PlaceResponse> items = all.subList(from, to);
        boolean hasNext = to < total;

        return ApiResponse.ok(new PlacePageResponse(items, page, size, total, hasNext));
    }

    @GetMapping("/search")
    public ApiResponse<PlacePageResponse> search(
            @RequestParam String query,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") @Min(0) @Max(3000) int radius,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size
    ) {
        PlacePageResponse result = placeService.searchPlaces(
                query, lat, lng, radius, page, size
        );
        return ApiResponse.ok(result);
    }

    /**
     * 장소 상세 조회
     * GET /api/places/{placeId}
     * - placeId 예: ext:KAKAO:123456
     * - 현재는 캐시 기반으로 조회(검색/nearby로 한번이라도 조회된 항목)
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
                    result.address(),
                    result.latitude(),
                    result.longitude()
            );
        }

        return ApiResponse.ok(result);
    }
}