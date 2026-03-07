package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.application.file.port.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private final FileRepository fileRepository;
    private final StoragePort storagePort;

    @Transactional
    public int cleanupStalePendingFiles(Duration maxAge, int batchSize) {
        Instant cutoff = Instant.now().minus(maxAge);
        int deletedCount = 0;

        while (true) {
            List<File> files = fileRepository.findByStatusAndCreatedAtBeforeAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
                    FileStatus.PENDING,
                    cutoff,
                    PageRequest.of(0, batchSize)
            );
            if (files.isEmpty()) {
                return deletedCount;
            }

            for (File file : files) {
                deleteStorageObjectQuietly(file);
                fileRepository.delete(file);
                deletedCount++;
            }
            fileRepository.flush();
        }
    }

    @Transactional
    public int cleanupExpiredDeletedFiles(Duration retention, int batchSize) {
        Instant cutoff = Instant.now().minus(retention);
        int deletedCount = 0;

        while (true) {
            List<File> files = fileRepository.findByDeletedAtBeforeOrderByDeletedAtAscIdAsc(
                    cutoff,
                    PageRequest.of(0, batchSize)
            );
            if (files.isEmpty()) {
                return deletedCount;
            }

            for (File file : files) {
                deleteStorageObjectQuietly(file);
                fileRepository.delete(file);
                deletedCount++;
            }
            fileRepository.flush();
        }
    }

    private void deleteStorageObjectQuietly(File file) {
        if (file.getFileKey() == null || file.getFileKey().isBlank()) {
            return;
        }

        try {
            storagePort.deleteObject(file.getFileKey());
        } catch (RuntimeException ex) {
            log.warn(
                    "event=file_cleanup_storage_delete_failed file_id={} file_key={} reason={}",
                    file.getId(),
                    file.getFileKey(),
                    ex.getMessage()
            );
        }
    }
}
