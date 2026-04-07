package com.example.capstone.domain.auth.controller;

import com.example.capstone.domain.auth.dto.request.KakaoLoginRequest;
import com.example.capstone.domain.auth.dto.request.ReissueRequest;
import com.example.capstone.domain.auth.dto.response.LoginResponse;
import com.example.capstone.domain.auth.service.AuthService;
import com.example.capstone.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[AUTH]")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao/login")
    public ApiResponse<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ApiResponse.ok(authService.loginWithKakaoAccessToken(request.kakaoAccessToken()));
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
