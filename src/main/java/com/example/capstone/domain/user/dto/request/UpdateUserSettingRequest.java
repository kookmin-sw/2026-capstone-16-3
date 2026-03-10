package com.example.capstone.domain.user.dto.request;

public record UpdateUserSettingRequest(
        Integer guidanceFrequency,
        Integer sentenceLength,
        Integer vibrationStrength,
        Boolean voiceGuidanceEnabled
) {
}
