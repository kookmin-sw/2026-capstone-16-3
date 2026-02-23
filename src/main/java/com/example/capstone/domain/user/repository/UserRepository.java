package com.example.capstone.domain.user.repository;

import com.example.capstone.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoUserId(String kakaoUserId);
    boolean existsByKakaoUserId(String kakaoUserId);
}
