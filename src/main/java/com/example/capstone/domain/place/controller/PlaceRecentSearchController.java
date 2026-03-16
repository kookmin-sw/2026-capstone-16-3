package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.PlaceRecentPageResponse;
import com.example.capstone.domain.place.service.PlaceRecentSearchService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/users/me/recent")
@RequiredArgsConstructor
public class PlaceRecentSearchController {

    private final PlaceRecentSearchService placeRecentSearchService;

    @GetMapping
    public ApiResponse<PlaceRecentPageResponse> getRecent(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(
                placeRecentSearchService.getRecent(String.valueOf(userId), page, size)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteOne(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                placeRecentSearchService.deleteOne(String.valueOf(userId), id)
        );
    }

    @DeleteMapping
    public ApiResponse<Integer> deleteAll(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(
                placeRecentSearchService.deleteAll(String.valueOf(userId))
        );
    }
}