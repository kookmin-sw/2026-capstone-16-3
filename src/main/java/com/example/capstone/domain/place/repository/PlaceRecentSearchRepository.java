package com.example.capstone.domain.place.repository;

import com.example.capstone.domain.place.entity.PlaceRecentSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRecentSearchRepository extends JpaRepository<PlaceRecentSearch, Long> {

    Page<PlaceRecentSearch> findByUserKeyOrderBySearchedAtDesc(String userKey, Pageable pageable);

    Optional<PlaceRecentSearch> findByUserKeyAndPlaceId(String userKey, String placeId);

    long countByUserKey(String userKey);

    int deleteByIdAndUserKey(Long id, String userKey);

    int deleteByUserKey(String userKey);
}