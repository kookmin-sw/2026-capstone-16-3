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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_length", nullable = false)
    private SentenceLength sentenceLength = SentenceLength.MEDIUM;

    @Builder.Default
    @Column(name = "vibration_strength", nullable = false)
    private Integer vibrationStrength = 50;

    @Builder.Default
    @Column(name = "voice_guidance_enabled", nullable = false)
    private Boolean voiceGuidanceEnabled = true;

    public void update(
            SentenceLength sentenceLength,
            Integer vibrationStrength,
            Boolean voiceGuidanceEnabled
    ) {
        if (sentenceLength != null) {
            this.sentenceLength = sentenceLength;
        }
        if (vibrationStrength != null) {
            this.vibrationStrength = vibrationStrength;
        }
        if (voiceGuidanceEnabled != null) {
            this.voiceGuidanceEnabled = voiceGuidanceEnabled;
        }
    }
}
