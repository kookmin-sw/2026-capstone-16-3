package com.example.capstone.domain.auth.controller;

import com.example.capstone.domain.auth.dto.request.ReissueRequest;
import com.example.capstone.domain.auth.dto.response.LoginResponse;
import com.example.capstone.domain.auth.service.AuthService;
import com.example.capstone.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/kakao/callback")
    public ApiResponse<LoginResponse> kakaoCallback(@RequestParam("code") String code) {
        return ApiResponse.ok(authService.loginWithKakao(code));
    }

    @PostMapping("/reissue")
    public ApiResponse<LoginResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return ApiResponse.ok(authService.reissue(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ApiResponse.ok();
    }
}
