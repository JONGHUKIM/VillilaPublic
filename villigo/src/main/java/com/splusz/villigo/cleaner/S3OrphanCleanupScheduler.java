package com.splusz.villigo.cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.splusz.villigo.cleaner.S3OrphanCleaner;

@Component
public class S3OrphanCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(S3OrphanCleanupScheduler.class);

    private final S3OrphanCleaner s3OrphanCleaner;

    public S3OrphanCleanupScheduler(S3OrphanCleaner s3OrphanCleaner) {
        this.s3OrphanCleaner = s3OrphanCleaner;
    }

    // 매주 일요일 4시에 실행
    @Scheduled(cron = "0 0 4 * * SUN")
    public void runCleanup() {
        log.info("스케줄된 S3 고아 이미지 클리너 작업 시작...");
        boolean dryRun = true; // 정확한 파일 확인 후 false로 변경 예정
        s3OrphanCleaner.cleanupAllFolders(dryRun);
        log.info("스케줄된 S3 고아 이미지 클리너 작업 완료.");
    }
}
