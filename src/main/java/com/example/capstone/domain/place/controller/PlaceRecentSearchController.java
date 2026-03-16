package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.PlaceRecentPageResponse;
import com.example.capstone.domain.place.service.PlaceRecentSearchService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/places/recent")
public class PlaceRecentSearchController {

    private final PlaceRecentSearchService placeRecentSearchService;

    public PlaceRecentSearchController(PlaceRecentSearchService placeRecentSearchService) {
        this.placeRecentSearchService = placeRecentSearchService;
    }

    @GetMapping
    public ApiResponse<PlaceRecentPageResponse> getRecent(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        String userKey = (userId == null) ? "anonymous" : String.valueOf(userId);
        return ApiResponse.ok(placeRecentSearchService.getRecent(userKey, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteOne(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        String userKey = (userId == null) ? "anonymous" : String.valueOf(userId);
        return ApiResponse.ok(placeRecentSearchService.deleteOne(userKey, id));
    }

    @DeleteMapping
    public ApiResponse<Integer> deleteAll(@AuthenticationPrincipal Long userId) {
        String userKey = (userId == null) ? "anonymous" : String.valueOf(userId);
        return ApiResponse.ok(placeRecentSearchService.deleteAll(userKey));
    }
}