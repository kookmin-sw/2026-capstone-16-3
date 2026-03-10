package com.example.capstone.domain.user.dto.response;

public record UserSettingResponse(
        Integer guidanceFrequency,
        Integer sentenceLength,
        Integer vibrationStrength,
        Boolean voiceGuidanceEnabled
) {
}
