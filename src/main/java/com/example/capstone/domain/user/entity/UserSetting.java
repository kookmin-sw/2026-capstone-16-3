package com.example.capstone.domain.user.entity;

import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_settings_user_id", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSetting extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "guidance_frequency", nullable = false)
    private Integer guidanceFrequency;

    @Column(name = "sentence_length", nullable = false)
    private Integer sentenceLength;

    @Column(name = "vibration_strength", nullable = false)
    private Integer vibrationStrength;

    @Column(name = "voice_guidance_enabled", nullable = false)
    private Boolean voiceGuidanceEnabled;
}
