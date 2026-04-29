package com.example.capstone.domain.crosswalk.repository;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrosswalkRepository extends JpaRepository<Crosswalk, Long> {

    Optional<Crosswalk> findByCrosswalkCode(String crosswalkCode);

    List<Crosswalk> findBySigungu(String sigungu);

    List<Crosswalk> findByBaseSource(DataSourceType baseSource);

    List<Crosswalk> findByBaseSourceAndSigungu(DataSourceType baseSource, String sigungu);
}