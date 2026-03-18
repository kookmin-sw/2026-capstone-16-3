package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.response.RecentPlaceDeleteAllResponse;
import com.example.capstone.domain.place.dto.response.RecentPlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.RecentPlacePageResponse;
import com.example.capstone.domain.place.service.RecentPlaceService;
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
public class RecentPlaceController {

    private final RecentPlaceService recentPlaceService;

    @GetMapping
    public ApiResponse<RecentPlacePageResponse> getRecent(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(
                recentPlaceService.getRecent(userId, page, size)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<RecentPlaceDeleteResponse> deleteOne(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                recentPlaceService.deleteOne(userId, id)
        );
    }

    @DeleteMapping
    public ApiResponse<RecentPlaceDeleteAllResponse> deleteAll(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.ok(
                recentPlaceService.deleteAll(userId)
        );
    }
}