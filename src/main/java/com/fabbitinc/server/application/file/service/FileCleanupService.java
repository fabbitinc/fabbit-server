package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.application.file.port.StorageObjectListPage;
import com.fabbitinc.server.application.file.port.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final String TENANT_PREFIX_FORMAT = "tenants/%s/";

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

    public int cleanupOrphanObjects(UUID orgId, int batchSize) {
        String prefix = TENANT_PREFIX_FORMAT.formatted(orgId);
        String continuationToken = null;
        int deletedCount = 0;

        while (true) {
            StorageObjectListPage page = storagePort.listObjects(prefix, continuationToken, batchSize);
            deletedCount += deleteOrphanObjects(page.objectKeys());

            if (!page.hasNextPage()) {
                return deletedCount;
            }
            continuationToken = page.nextContinuationToken();
        }
    }

    private int deleteOrphanObjects(List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return 0;
        }

        Set<String> existingKeys = fileRepository.findByFileKeyIn(objectKeys).stream()
                .map(File::getFileKey)
                .collect(Collectors.toSet());

        int deletedCount = 0;
        for (String objectKey : objectKeys) {
            if (existingKeys.contains(objectKey)) {
                continue;
            }
            if (deleteStorageObjectQuietly(objectKey)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private void deleteStorageObjectQuietly(File file) {
        if (file.getFileKey() == null || file.getFileKey().isBlank()) {
            return;
        }

        deleteStorageObjectQuietly(file.getFileKey());
    }

    private boolean deleteStorageObjectQuietly(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return false;
        }

        try {
            storagePort.deleteObject(fileKey);
            return true;
        } catch (RuntimeException ex) {
            log.warn(
                    "event=file_cleanup_storage_delete_failed file_key={} reason={}",
                    fileKey,
                    ex.getMessage()
            );
            return false;
        }
    }
}
