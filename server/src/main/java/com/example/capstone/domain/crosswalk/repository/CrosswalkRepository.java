package com.example.capstone.domain.crosswalk.repository;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrosswalkRepository extends JpaRepository<Crosswalk, Long> {

    Optional<Crosswalk> findByCrosswalkCode(String crosswalkCode);

    List<Crosswalk> findBySigungu(String sigungu);

    List<Crosswalk> findByBaseSource(DataSourceType baseSource);

    List<Crosswalk> findByBaseSourceAndSigungu(DataSourceType baseSource, String sigungu);

    List<Crosswalk> findByBaseSourceAndSidoAndSigungu(
            DataSourceType baseSource,
            String sido,
            String sigungu
    );

    @Query("""
            select crosswalk
            from Crosswalk crosswalk
            where crosswalk.latitude between :minLatitude and :maxLatitude
              and crosswalk.longitude between :minLongitude and :maxLongitude
            """)
    List<Crosswalk> findCandidatesByBoundingBox(
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude
    );
}