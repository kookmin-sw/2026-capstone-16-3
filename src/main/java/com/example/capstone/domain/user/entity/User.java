package com.example.capstone.domain.user.entity;

import com.example.capstone.domain.place.entity.FavoritePlace;
import com.example.capstone.domain.place.entity.RecentPlace;
import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_kakao_user_id", columnList = "kakao_user_id", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "kakao_user_id", nullable = false, unique = true, length = 255)
    private String kakaoUserId;

    @Column(name = "nickname", length = 255)
    private String nickname;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSetting userSetting;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("searchedAt DESC")
    @Builder.Default
    private List<RecentPlace> recentSearchPlaces = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FavoritePlace> favoritePlaces = new ArrayList<>();
}