package com.example.capstone.domain.crosswalk.scheduler;

import com.example.capstone.domain.crosswalk.service.CrosswalkDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrosswalkDataSyncScheduler {

    private final CrosswalkDataSyncService crosswalkDataSyncService;

    @Scheduled(cron = "0 0 3 ? * MON", zone = "Asia/Seoul")
    public void syncBiweekly() {
        int weekOfYear = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        if (weekOfYear % 2 != 0) {
            return;
        }

        log.info("[CROSSWALK SYNC START] biweekly schedule started. weekOfYear={}", weekOfYear);
        crosswalkDataSyncService.syncAll();
    }
}