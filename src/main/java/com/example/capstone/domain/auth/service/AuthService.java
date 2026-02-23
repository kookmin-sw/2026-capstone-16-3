package com.example.capstone.domain.auth.service;

import com.example.capstone.domain.auth.dto.response.KakaoTokenResponse;
import com.example.capstone.domain.auth.dto.response.KakaoUserResponse;
import com.example.capstone.domain.auth.dto.response.LoginResponse;
import com.example.capstone.domain.auth.entity.RefreshToken;
import com.example.capstone.domain.auth.repository.RefreshTokenRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.global.exception.BusinessException;
import com.example.capstone.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String GRANT_TYPE = "Bearer";
    private static final int REFRESH_EXPIRE_DAYS = 14;

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse loginWithKakao(String code) {
        KakaoTokenResponse token = kakaoOAuthClient.getToken(code);
        KakaoUserResponse userInfo = kakaoOAuthClient.getUserInfo(token.access_token());

        String kakaoUserId = String.valueOf(userInfo.id());
        String nickname = extractNickname(userInfo);

        User user = userRepository.findByKakaoUserId(kakaoUserId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoUserId(kakaoUserId)
                                .nickname(nickname)
                                .build()
                ));

        String accessJwt = jwtProvider.createAccessToken(user.getId());
        String refreshJwt = jwtProvider.createRefreshToken(user.getId());

        LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(REFRESH_EXPIRE_DAYS);

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        existing -> existing.updateToken(refreshJwt, refreshExpiry),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .user(user)
                                        .token(refreshJwt)
                                        .expiryDate(refreshExpiry)
                                        .build()
                        )
                );

        Long accessTokenExpiresIn = jwtProvider.getExpiration(accessJwt).getTime();

        return new LoginResponse(
                GRANT_TYPE,
                accessJwt,
                refreshJwt,
                accessTokenExpiresIn
        );
    }

    @Transactional
    public LoginResponse reissue(String refreshToken) {
        // 1) JWT 형식/서명/만료 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException("AUTH_INVALID_TOKEN", "토큰이 유효하지 않습니다.");
        }

        // 2) Refresh 토큰인지 타입 체크(권장)
        if (!"REFRESH".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new BusinessException("AUTH_TOKEN_TYPE", "Refresh 토큰이 필요합니다.");
        }

        // 3) DB에 저장된 refresh token인지 확인
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("AUTH_REFRESH_NOT_FOUND", "Refresh 토큰이 존재하지 않습니다."));

        // 4) DB 기준 만료 확인
        if (savedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("AUTH_REFRESH_EXPIRED", "만료된 Refresh 토큰입니다.");
        }

        User user = savedToken.getUser();

        // 5) 새 토큰 발급 + refresh rotation
        String newAccess = jwtProvider.createAccessToken(user.getId());
        String newRefresh = jwtProvider.createRefreshToken(user.getId());

        LocalDateTime newRefreshExpiry = LocalDateTime.now().plusDays(REFRESH_EXPIRE_DAYS);
        savedToken.updateToken(newRefresh, newRefreshExpiry);

        Long accessTokenExpiresIn = jwtProvider.getExpiration(newAccess).getTime();

        return new LoginResponse(
                GRANT_TYPE,
                newAccess,
                newRefresh,
                accessTokenExpiresIn
        );
    }

    private String extractNickname(KakaoUserResponse userInfo) {
        if (userInfo.kakao_account() != null
                && userInfo.kakao_account().profile() != null
                && userInfo.kakao_account().profile().nickname() != null) {
            return userInfo.kakao_account().profile().nickname();
        }
        if (userInfo.properties() != null && userInfo.properties().nickname() != null) {
            return userInfo.properties().nickname();
        }
        return null;
    }
}
