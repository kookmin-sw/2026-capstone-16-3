package com.example.capstone.domain.user.dto.response;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String kakaoUserId,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
