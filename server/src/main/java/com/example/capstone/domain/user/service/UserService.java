package com.example.capstone.domain.user.service;

import com.example.capstone.domain.auth.repository.RefreshTokenRepository;
import com.example.capstone.domain.place.repository.FavoritePlaceRepository;
import com.example.capstone.domain.place.repository.RecentPlaceRepository;
import com.example.capstone.domain.user.dto.response.DeleteUserResponse;
import com.example.capstone.domain.user.dto.response.ProfileResponse;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.exception.UserErrorCode;
import com.example.capstone.domain.user.exception.UserException;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.domain.user.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FavoritePlaceRepository favoritePlaceRepository;
    private final RecentPlaceRepository recentPlaceRepository;
    private final UserSettingRepository userSettingRepository;

    public ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return new ProfileResponse(
                user.getId(),
                user.getKakaoUserId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public DeleteUserResponse deleteMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUser(user);
        favoritePlaceRepository.deleteByUser(user);
        recentPlaceRepository.deleteByUser(user);
        userSettingRepository.deleteByUser(user);

        userRepository.delete(user);

        return new DeleteUserResponse(true);
    }
}
