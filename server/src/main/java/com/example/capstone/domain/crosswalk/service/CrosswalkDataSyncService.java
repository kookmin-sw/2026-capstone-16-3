package com.example.capstone.domain.crosswalk.service;

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
        boolean nationalSuccess = runCollector("전국 횡단보도", nationalCrosswalkCollector::collectAndSave);
        boolean seoulSuccess = runCollector("서울 횡단보도", seoulCrosswalkCollector::collectAndSave);
        boolean acousticSuccess = runCollector("서울 음향신호기", seoulAcousticSignalCollector::collectAndSave);

        if (seoulSuccess || nationalSuccess) {
            crosswalkMergeService.mergeCrosswalkData();
        }

        if ((seoulSuccess || nationalSuccess) && acousticSuccess) {
            matchService.matchCrosswalkAndSignals();
        }
    }

    private boolean runCollector(String name, Runnable collector) {
        try {
            collector.run();
            return true;
        } catch (Exception e) {
            log.error("[CROSSWALK SYNC FAIL] collector={}", name, e);
            return false;
        }
    }
}