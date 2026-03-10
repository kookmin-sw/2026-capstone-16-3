package com.example.capstone.domain.user.service;

import com.example.capstone.domain.user.dto.request.UpdateUserSettingRequest;
import com.example.capstone.domain.user.dto.response.UserSettingResponse;
import com.example.capstone.domain.user.entity.UserSetting;
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
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;

    public UserSettingResponse getUserSetting(Long userId) {

        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_SETTING_NOT_FOUND));

        return new UserSettingResponse(
                setting.getGuidanceFrequency(),
                setting.getSentenceLength(),
                setting.getVibrationStrength(),
                setting.getVoiceGuidanceEnabled()
        );
    }

    @Transactional
    public UserSettingResponse updateUserSetting(Long userId, UpdateUserSettingRequest request) {

        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_SETTING_NOT_FOUND));

        setting.update(
                request.guidanceFrequency(),
                request.sentenceLength(),
                request.vibrationStrength(),
                request.voiceGuidanceEnabled()
        );

        return new UserSettingResponse(
                setting.getGuidanceFrequency(),
                setting.getSentenceLength(),
                setting.getVibrationStrength(),
                setting.getVoiceGuidanceEnabled()
        );
    }
}
