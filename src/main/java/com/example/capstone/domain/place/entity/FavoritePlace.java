package com.example.capstone.domain.place.entity;

import com.example.capstone.domain.user.entity.User;
import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "favorite_places",
        indexes = {
                @Index(name = "idx_favorite_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FavoritePlace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "address", length = 500)
    private String address;
}
