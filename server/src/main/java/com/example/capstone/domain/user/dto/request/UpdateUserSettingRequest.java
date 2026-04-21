package com.example.capstone.domain.user.dto.request;

import com.example.capstone.domain.user.entity.SentenceLength;

public record UpdateUserSettingRequest(
        SentenceLength sentenceLength,
        Integer vibrationStrength,
        Boolean voiceGuidanceEnabled
) {
}
