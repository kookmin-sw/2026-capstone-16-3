package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.service.sync.CrosswalkSyncStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrosswalkDataSyncService {

    private final SeoulCrosswalkCollector seoulCrosswalkCollector;
    private final NationalCrosswalkCollector nationalCrosswalkCollector;
    private final SeoulAcousticSignalCollector seoulAcousticSignalCollector;
    private final CrosswalkMergeService crosswalkMergeService;
    private final CrosswalkAcousticSignalMatchService matchService;

    public void syncAll() {
        CrosswalkSyncStats nationalStats = runCollector("전국 횡단보도", nationalCrosswalkCollector);
        CrosswalkSyncStats seoulStats = runCollector("서울 횡단보도", seoulCrosswalkCollector);
        CrosswalkSyncStats acousticStats = runCollector("서울 음향신호기", seoulAcousticSignalCollector);

        boolean nationalSuccess = nationalStats != null;
        boolean seoulSuccess = seoulStats != null;
        boolean acousticSuccess = acousticStats != null;

        if (seoulSuccess || nationalSuccess) {
            crosswalkMergeService.mergeCrosswalkData();
        }

        if ((seoulSuccess || nationalSuccess) && acousticSuccess) {
            matchService.matchCrosswalkAndSignals();
        }

        log.info(
                "[CROSSWALK SYNC SUMMARY] national={}, seoul={}, acoustic={}",
                formatStats(nationalStats),
                formatStats(seoulStats),
                formatStats(acousticStats)
        );
    }

    private CrosswalkSyncStats runCollector(String name, com.example.capstone.domain.crosswalk.service.sync.PublicDataCollector collector) {
        try {
            CrosswalkSyncStats stats = collector.collectAndSave();

            log.info(
                    "[CROSSWALK COLLECT SUMMARY] source={}, total={}, success={}, failure={}, failureReasons={}",
                    stats.sourceName(),
                    stats.totalCount(),
                    stats.successCount(),
                    stats.failureCount(),
                    stats.failureSummary()
            );

            return stats;
        } catch (Exception e) {
            log.error("[CROSSWALK SYNC FAIL] collector={}", name, e);
            return null;
        }
    }

    private String formatStats(CrosswalkSyncStats stats) {
        if (stats == null) {
            return "FAILED";
        }

        return String.format(
                "{source=%s, total=%d, success=%d, failure=%d, failureReasons=%s}",
                stats.sourceName(),
                stats.totalCount(),
                stats.successCount(),
                stats.failureCount(),
                stats.failureSummary()
        );
    }
}