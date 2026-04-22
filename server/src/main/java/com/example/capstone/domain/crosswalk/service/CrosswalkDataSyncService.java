package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.service.sync.CrosswalkAcousticSignalMatchService;
import com.example.capstone.domain.crosswalk.service.sync.CrosswalkMergeService;
import com.example.capstone.domain.crosswalk.service.sync.NationalCrosswalkCollector;
import com.example.capstone.domain.crosswalk.service.sync.SeoulAcousticSignalCollector;
import com.example.capstone.domain.crosswalk.service.SeoulCrosswalkCollectorStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CrosswalkDataSyncService {

    private final SeoulCrosswalkCollectorStub seoulCrosswalkCollector;
    private final NationalCrosswalkCollector nationalCrosswalkCollector;
    private final SeoulAcousticSignalCollector seoulAcousticSignalCollector;
    private final CrosswalkMergeService crosswalkMergeService;
    private final CrosswalkAcousticSignalMatchService matchService;

    public void syncAll() {
        seoulCrosswalkCollector.collectAndSave();
        nationalCrosswalkCollector.collectAndSave();
        seoulAcousticSignalCollector.collectAndSave();

        crosswalkMergeService.mergeCrosswalkData();
        matchService.matchCrosswalkAndSignals();
    }
}