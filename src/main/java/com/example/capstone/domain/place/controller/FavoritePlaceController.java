package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.request.FavoritePlaceCreateRequest;
import com.example.capstone.domain.place.dto.response.FavoritePlaceCreateResponse;
import com.example.capstone.domain.place.dto.response.FavoritePlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.FavoritePlacePageResponse;
import com.example.capstone.domain.place.service.FavoritePlaceService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class FavoritePlaceController {

    private final FavoritePlaceService favoritePlaceService;

    @GetMapping
    public ApiResponse<FavoritePlacePageResponse> getFavorites(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(
                favoritePlaceService.getFavorites(userId, page, size)
        );
    }

    @PostMapping
    public ApiResponse<FavoritePlaceCreateResponse> createFavorite(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FavoritePlaceCreateRequest request
    ) {
        return ApiResponse.ok(
                favoritePlaceService.createFavorite(userId, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<FavoritePlaceDeleteResponse> deleteFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                favoritePlaceService.deleteFavorite(userId, id)
        );
    }
}