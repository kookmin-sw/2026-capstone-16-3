package com.example.capstone.domain.crosswalk.repository;

import com.example.capstone.domain.crosswalk.entity.AcousticSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcousticSignalRepository extends JpaRepository<AcousticSignal, Long> {

    Optional<AcousticSignal> findByAcousticSignalCode(String acousticSignalCode);
}