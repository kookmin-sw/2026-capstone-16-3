package com.example.capstone.domain.user.service;

import com.example.capstone.domain.user.dto.response.ProfileResponse;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.exception.UserErrorCode;
import com.example.capstone.domain.user.exception.UserException;
import com.example.capstone.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
}
