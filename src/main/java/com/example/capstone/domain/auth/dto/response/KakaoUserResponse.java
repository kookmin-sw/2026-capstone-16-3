package com.example.capstone.domain.auth.dto.response;

public record KakaoUserResponse(
        Long id,
        KakaoAccount kakao_account,
        Properties properties
) {
    public record KakaoAccount(String email, Profile profile) {
        public record Profile(String nickname) {}
    }
    public record Properties(String nickname) {}
}
