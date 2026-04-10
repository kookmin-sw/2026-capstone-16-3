package com.example.capstone.domain.place.controller;

import com.example.capstone.domain.place.dto.request.FavoritePlaceCreateRequest;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlaceCreateResponse;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlaceDeleteResponse;
import com.example.capstone.domain.place.dto.response.favorite.FavoritePlacePageResponse;
import com.example.capstone.domain.place.service.FavoritePlaceService;
import com.example.capstone.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@SecurityRequirement(name = "bearerAuth")
@RestController
@Tag(name = "[즐겨찾기 장소]")
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class FavoritePlaceController {

    private final FavoritePlaceService favoritePlaceService;

    @GetMapping
    public ApiResponse<FavoritePlacePageResponse> getFavorites(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(50) int size
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