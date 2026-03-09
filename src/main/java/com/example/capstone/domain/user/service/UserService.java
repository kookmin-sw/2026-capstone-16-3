package com.example.capstone.domain.user.service;

import com.example.capstone.domain.user.dto.response.ProfileResponse;
import com.example.capstone.domain.user.entity.User;
import com.example.capstone.domain.user.repository.UserRepository;
import com.example.capstone.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        return new ProfileResponse(
                user.getId(),
                user.getKakaoUserId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
