package com.example.capstone.domain.user.controller;

import com.example.capstone.domain.user.dto.response.ProfileResponse;
import com.example.capstone.domain.user.service.UserService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getMyProfile(userId));
    }
}
