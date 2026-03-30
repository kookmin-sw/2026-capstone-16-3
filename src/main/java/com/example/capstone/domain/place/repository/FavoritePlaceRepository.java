package com.example.capstone.domain.place.repository;

import com.example.capstone.domain.place.entity.FavoritePlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritePlaceRepository extends JpaRepository<FavoritePlace, Long> {

    Page<FavoritePlace> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndPlaceId(Long userId, String placeId);

    long countByUserId(Long userId);

    int deleteByIdAndUserId(Long id, Long userId);
}