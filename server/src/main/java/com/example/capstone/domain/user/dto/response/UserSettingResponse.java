package com.example.capstone.domain.user.dto.response;

import com.example.capstone.domain.user.entity.SentenceLength;

public record UserSettingResponse(
        SentenceLength sentenceLength,
        Integer vibrationStrength,
        Boolean voiceGuidanceEnabled,
        Double guidanceSpeed
) {
}
