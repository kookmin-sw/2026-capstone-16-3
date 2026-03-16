package com.example.capstone.domain.place.entity;

import com.example.capstone.domain.user.entity.User;
import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "favorite_places",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_place_user_place",
                        columnNames = {"user_id", "place_id"}
                )
        },
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

    @Column(name = "place_id", nullable = false, length = 128)
    private String placeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "alias", length = 255)
    private String alias;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;
}