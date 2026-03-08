package com.fabbitinc.server.infrastructure.scheduling;

import com.fabbitinc.server.application.drawing.service.DrawingTempDirectoryCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawingTempDirectoryCleanupScheduler {

    private final DrawingTempDirectoryCleanupService drawingTempDirectoryCleanupService;

    @Scheduled(cron = "${app.drawing-converter.temp-dir-cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredWorkDirectories() {
        try {
            int deletedCount = drawingTempDirectoryCleanupService.cleanupExpiredWorkDirectories();
            if (deletedCount > 0) {
                log.info(
                        "event=drawing_temp_dir_cleanup_completed deleted_count={} outcome=success",
                        deletedCount
                );
            }
        } catch (Exception ex) {
            log.error(
                    "event=drawing_temp_dir_cleanup_failed reason={}",
                    ex.getMessage(),
                    ex
            );
        }
    }
}
