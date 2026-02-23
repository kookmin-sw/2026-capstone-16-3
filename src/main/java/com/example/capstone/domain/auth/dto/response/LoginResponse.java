package com.example.capstone.domain.auth.dto.response;

public record LoginResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {
}
