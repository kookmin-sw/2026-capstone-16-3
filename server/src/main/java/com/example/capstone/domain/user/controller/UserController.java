package com.example.capstone.domain.user.controller;

import com.example.capstone.domain.user.dto.request.UpdateUserSettingRequest;
import com.example.capstone.domain.user.dto.response.DeleteUserResponse;
import com.example.capstone.domain.user.dto.response.ProfileResponse;
import com.example.capstone.domain.user.dto.response.UserSettingResponse;
import com.example.capstone.domain.user.service.UserService;
import com.example.capstone.domain.user.service.UserSettingService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final UserSettingService userSettingService;

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getMyProfile(userId));
    }

    @GetMapping("/settings")
    public ApiResponse<UserSettingResponse> getUserSetting(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userSettingService.getUserSetting(userId));
    }

    @PatchMapping("/settings")
    public ApiResponse<UserSettingResponse> updateUserSetting(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateUserSettingRequest request
            ) {
        return ApiResponse.ok(userSettingService.updateUserSetting(userId, request));
    }

    @DeleteMapping("/profile")
    public ApiResponse<DeleteUserResponse> deleteMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.deleteMyProfile(userId));
    }
}
