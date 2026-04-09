package com.example.capstone.domain.user.dto.response;

public record UserSettingResponse(
        Integer sentenceLength,
        Integer vibrationStrength,
        Boolean voiceGuidanceEnabled
) {
}
