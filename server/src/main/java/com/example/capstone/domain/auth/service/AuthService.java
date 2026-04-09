package com.example.capstone.domain.auth.service;

import com.example.capstone.domain.auth.dto.response.KakaoUserResponse;
import com.example.capstone.domain.auth.dto.response.LoginResponse;
import com.example.capstone.domain.auth.entity.RefreshToken;
import com.example.capstone.domain.auth.repository.RefreshTokenRepository;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.entity.UserSetting;
import com.example.capstone.domain.user.exception.UserErrorCode;
import com.example.capstone.domain.user.exception.UserException;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.domain.user.repository.UserSettingRepository;
import com.example.capstone.global.exception.BusinessException;
import com.example.capstone.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String GRANT_TYPE = "Bearer";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse loginWithKakaoAccessToken(String kakaoAccessToken) {
        KakaoUserResponse userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);

        String kakaoUserId = String.valueOf(userInfo.id());
        String nickname = extractNickname(userInfo);

        User user = userRepository.findByKakaoUserId(kakaoUserId)
                .orElseGet(() -> createUserWithDefaultSetting(kakaoUserId, nickname));

        String accessJwt = jwtProvider.createAccessToken(user.getId());
        String refreshJwt = jwtProvider.createRefreshToken(user.getId());

        LocalDateTime refreshExpiry = toLocalDateTime(jwtProvider.getExpiration(refreshJwt));

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
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException("AUTH_INVALID_TOKEN", "토큰이 유효하지 않습니다.");
        }

        if (!"REFRESH".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new BusinessException("AUTH_TOKEN_TYPE", "Refresh 토큰이 필요합니다.");
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("AUTH_REFRESH_NOT_FOUND", "Refresh 토큰이 존재하지 않습니다."));

        if (savedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("AUTH_REFRESH_EXPIRED", "만료된 Refresh 토큰입니다.");
        }

        User user = savedToken.getUser();

        String newAccess = jwtProvider.createAccessToken(user.getId());
        String newRefresh = jwtProvider.createRefreshToken(user.getId());

        // 새 refresh token의 JWT exp 기준으로 저장
        LocalDateTime newRefreshExpiry = toLocalDateTime(jwtProvider.getExpiration(newRefresh));
        savedToken.updateToken(newRefresh, newRefreshExpiry);

        Long accessTokenExpiresIn = jwtProvider.getExpiration(newAccess).getTime();

        return new LoginResponse(
                GRANT_TYPE,
                newAccess,
                newRefresh,
                accessTokenExpiresIn
        );
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUser(user);
    }

    private String extractNickname(KakaoUserResponse userInfo) {
        if (userInfo.kakao_account() != null
                && userInfo.kakao_account().profile() != null
                && userInfo.kakao_account().profile().nickname() != null) {
            return userInfo.kakao_account().profile().nickname();
        }

        if (userInfo.properties() != null
                && userInfo.properties().nickname() != null) {
            return userInfo.properties().nickname();
        }

        return null;
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private User createUserWithDefaultSetting(String kakaoUserId, String nickname) {
        User user = userRepository.save(
                User.builder()
                        .kakaoUserId(kakaoUserId)
                        .nickname(nickname)
                        .build()
        );

        UserSetting userSetting = UserSetting.builder()
                .user(user)
                .build();

        userSettingRepository.save(userSetting);

        return user;
    }
}
