package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.application.file.port.StorageDeleteResult;
import com.fabbitinc.server.application.file.port.StorageObjectListPage;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.file.service.input.CleanupOrphanObjectsInput;
import com.fabbitinc.server.application.file.service.output.CleanupOrphanObjectsOutput;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final String TENANT_PREFIX_FORMAT = "tenants/%s/";

    private final FileRepository fileRepository;
    private final StoragePort storagePort;

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

            deleteStorageObjectsQuietly(extractDeletableKeys(files));
            fileRepository.deleteAll(files);
            deletedCount += files.size();
        }
    }

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

            deleteStorageObjectsQuietly(extractDeletableKeys(files));
            fileRepository.deleteAll(files);
            deletedCount += files.size();
        }
    }

    public CleanupOrphanObjectsOutput cleanupOrphanObjects(CleanupOrphanObjectsInput input) {
        String prefix = TENANT_PREFIX_FORMAT.formatted(input.orgId());
        String continuationToken = null;
        int deletedCount = 0;
        int scannedPageCount = 0;
        int scannedObjectCount = 0;
        boolean stoppedByPageLimit = false;
        boolean stoppedByDeleteLimit = false;

        while (true) {
            if (scannedPageCount >= input.maxListPages()) {
                stoppedByPageLimit = true;
                break;
            }
            if (deletedCount >= input.maxDeleteCount()) {
                stoppedByDeleteLimit = true;
                break;
            }

            StorageObjectListPage page = storagePort.listObjects(prefix, continuationToken, input.listBatchSize());
            scannedPageCount++;
            scannedObjectCount += page.objectKeys().size();
            deletedCount += deleteOrphanObjects(
                    page.objectKeys(),
                    input.maxDeleteCount() - deletedCount
            );

            if (deletedCount >= input.maxDeleteCount()) {
                stoppedByDeleteLimit = true;
                break;
            }

            if (!page.hasNextPage()) {
                break;
            }
            continuationToken = page.nextContinuationToken();
            if (!pauseBetweenPages(input.pauseBetweenPages())) {
                break;
            }
        }

        log.info(
                "event=file_cleanup_orphan_completed org_id={} scanned_page_count={} scanned_object_count={} deleted_count={} stopped_by_page_limit={} stopped_by_delete_limit={} outcome=success",
                input.orgId(),
                scannedPageCount,
                scannedObjectCount,
                deletedCount,
                stoppedByPageLimit,
                stoppedByDeleteLimit
        );
        return new CleanupOrphanObjectsOutput(
                deletedCount,
                scannedPageCount,
                scannedObjectCount,
                stoppedByPageLimit,
                stoppedByDeleteLimit
        );
    }

    private int deleteOrphanObjects(List<String> objectKeys, int remainingDeleteBudget) {
        if (objectKeys.isEmpty() || remainingDeleteBudget <= 0) {
            return 0;
        }

        Set<String> existingKeys = fileRepository.findByFileKeyIn(objectKeys).stream()
                .map(File::getFileKey)
                .collect(Collectors.toSet());

        List<String> orphanKeys = objectKeys.stream()
                .filter(objectKey -> !existingKeys.contains(objectKey))
                .limit(remainingDeleteBudget)
                .toList();
        if (orphanKeys.isEmpty()) {
            return 0;
        }

        return deleteStorageObjectsQuietly(orphanKeys);
    }

    private List<String> extractDeletableKeys(List<File> files) {
        return files.stream()
                .map(File::getFileKey)
                .filter(fileKey -> fileKey != null && !fileKey.isBlank())
                .distinct()
                .toList();
    }

    private int deleteStorageObjectsQuietly(List<String> fileKeys) {
        if (fileKeys.isEmpty()) {
            return 0;
        }
        try {
            StorageDeleteResult result = storagePort.deleteObjects(fileKeys);
            if (!result.failedKeys().isEmpty()) {
                log.warn(
                        "event=file_cleanup_storage_bulk_delete_partial_failure requested_count={} deleted_count={} failed_count={}",
                        fileKeys.size(),
                        result.deletedCount(),
                        result.failedKeys().size()
                );
            }
            return result.deletedCount();
        } catch (RuntimeException ex) {
            log.warn(
                    "event=file_cleanup_storage_bulk_delete_failed requested_count={} reason={}",
                    fileKeys.size(),
                    ex.getMessage()
            );
            return 0;
        }
    }

    private boolean pauseBetweenPages(Duration pauseBetweenPages) {
        if (pauseBetweenPages == null || pauseBetweenPages.isZero() || pauseBetweenPages.isNegative()) {
            return true;
        }
        try {
            Thread.sleep(pauseBetweenPages.toMillis());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("event=file_cleanup_orphan_interrupted reason={}", ex.getMessage());
            return false;
        }
    }
}
