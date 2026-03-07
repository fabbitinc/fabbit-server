package com.fabbitinc.server.application.file.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private StoragePort storagePort;

    @InjectMocks
    private FileCleanupService fileCleanupService;

    @Test
    void cleanupStalePendingFiles_오래된_pending_파일을_스토리지와_DB에서_정리한다() {
        File first = File.create(UUID.randomUUID(), "a.txt", "tenants/org/uploaded/a.txt", "text/plain", 10);
        File second = File.create(UUID.randomUUID(), "b.txt", "tenants/org/uploaded/b.txt", "text/plain", 20);
        List<String> fileKeys = List.of(first.getFileKey(), second.getFileKey());

        when(fileRepository.findByStatusAndCreatedAtBeforeAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
                eq(FileStatus.PENDING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(first, second), List.of());
        when(storagePort.deleteObjects(fileKeys))
                .thenReturn(new StorageDeleteResult(fileKeys, List.of()));

        int deletedCount = fileCleanupService.cleanupStalePendingFiles(Duration.ofHours(24), 100);

        assertEquals(2, deletedCount);
        verify(storagePort).deleteObjects(fileKeys);
        verify(fileRepository).deleteAll(List.of(first, second));
    }

    @Test
    void cleanupExpiredDeletedFiles_bulk_delete가_실패해도_DB_정리는_진행한다() {
        File deleted = File.create(UUID.randomUUID(), "deleted.txt", "tenants/org/uploaded/deleted.txt", "text/plain", 30);
        deleted.markUploaded();
        deleted.softDelete();

        when(fileRepository.findByDeletedAtBeforeOrderByDeletedAtAscIdAsc(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(deleted), List.of());
        when(storagePort.deleteObjects(List.of(deleted.getFileKey())))
                .thenThrow(new RuntimeException("storage error"));

        int deletedCount = fileCleanupService.cleanupExpiredDeletedFiles(Duration.ofDays(7), 100);

        assertEquals(1, deletedCount);
        verify(storagePort).deleteObjects(List.of(deleted.getFileKey()));
        verify(fileRepository).deleteAll(List.of(deleted));
    }

    @Test
    void cleanupOrphanObjects_DB에없는_스토리지객체만_삭제한다() {
        UUID orgId = UUID.randomUUID();
        String prefix = "tenants/" + orgId + "/";
        String existingKey = prefix + "uploaded/existing.txt";
        String orphanKey = prefix + "uploaded/orphan.txt";
        String orphanKeyOnNextPage = prefix + "raw_data/orphan-next.csv";

        File existing = File.create(UUID.randomUUID(), "existing.txt", existingKey, "text/plain", 10);

        when(storagePort.listObjects(prefix, null, 100))
                .thenReturn(new StorageObjectListPage(List.of(existingKey, orphanKey), "next-page"));
        when(storagePort.listObjects(prefix, "next-page", 100))
                .thenReturn(new StorageObjectListPage(List.of(orphanKeyOnNextPage), null));
        when(fileRepository.findByFileKeyIn(List.of(existingKey, orphanKey)))
                .thenReturn(List.of(existing));
        when(fileRepository.findByFileKeyIn(List.of(orphanKeyOnNextPage)))
                .thenReturn(List.of());
        when(storagePort.deleteObjects(List.of(orphanKey)))
                .thenReturn(new StorageDeleteResult(List.of(orphanKey), List.of()));
        when(storagePort.deleteObjects(List.of(orphanKeyOnNextPage)))
                .thenReturn(new StorageDeleteResult(List.of(orphanKeyOnNextPage), List.of()));

        CleanupOrphanObjectsOutput output = fileCleanupService.cleanupOrphanObjects(new CleanupOrphanObjectsInput(
                orgId,
                100,
                20,
                500,
                Duration.ZERO
        ));

        assertEquals(2, output.deletedCount());
        assertEquals(2, output.scannedPageCount());
        assertEquals(3, output.scannedObjectCount());
        verify(storagePort).listObjects(eq(prefix), isNull(), eq(100));
        verify(storagePort).listObjects(prefix, "next-page", 100);
        verify(storagePort).deleteObjects(List.of(orphanKey));
        verify(storagePort).deleteObjects(List.of(orphanKeyOnNextPage));
    }

    @Test
    void cleanupOrphanObjects_일부_삭제가_실패해도_다음_orphan_정리는_계속한다() {
        UUID orgId = UUID.randomUUID();
        String prefix = "tenants/" + orgId + "/";
        String failedOrphanKey = prefix + "uploaded/failed.txt";
        String deletedOrphanKey = prefix + "uploaded/deleted.txt";

        when(storagePort.listObjects(prefix, null, 100))
                .thenReturn(new StorageObjectListPage(List.of(failedOrphanKey, deletedOrphanKey), null));
        when(fileRepository.findByFileKeyIn(List.of(failedOrphanKey, deletedOrphanKey)))
                .thenReturn(List.of());
        when(storagePort.deleteObjects(List.of(failedOrphanKey, deletedOrphanKey)))
                .thenReturn(new StorageDeleteResult(List.of(deletedOrphanKey), List.of(failedOrphanKey)));

        CleanupOrphanObjectsOutput output = fileCleanupService.cleanupOrphanObjects(new CleanupOrphanObjectsInput(
                orgId,
                100,
                20,
                500,
                Duration.ZERO
        ));

        assertEquals(1, output.deletedCount());
        verify(storagePort).deleteObjects(List.of(failedOrphanKey, deletedOrphanKey));
    }

    @Test
    void cleanupOrphanObjects_런당_페이지수를_제한한다() {
        UUID orgId = UUID.randomUUID();
        String prefix = "tenants/" + orgId + "/";
        String orphanKey = prefix + "uploaded/orphan.txt";

        when(storagePort.listObjects(prefix, null, 100))
                .thenReturn(new StorageObjectListPage(List.of(orphanKey), "next-page"));
        when(fileRepository.findByFileKeyIn(List.of(orphanKey)))
                .thenReturn(List.of());
        when(storagePort.deleteObjects(List.of(orphanKey)))
                .thenReturn(new StorageDeleteResult(List.of(orphanKey), List.of()));

        CleanupOrphanObjectsOutput output = fileCleanupService.cleanupOrphanObjects(new CleanupOrphanObjectsInput(
                orgId,
                100,
                1,
                500,
                Duration.ZERO
        ));

        assertEquals(1, output.deletedCount());
        assertEquals(1, output.scannedPageCount());
        assertEquals(1, output.scannedObjectCount());
        assertEquals(true, output.stoppedByPageLimit());
        verify(storagePort, times(1)).listObjects(eq(prefix), isNull(), eq(100));
    }

    @Test
    void cleanupOrphanObjects_런당_삭제수를_제한한다() {
        UUID orgId = UUID.randomUUID();
        String prefix = "tenants/" + orgId + "/";
        String orphanKey1 = prefix + "uploaded/orphan-1.txt";
        String orphanKey2 = prefix + "uploaded/orphan-2.txt";
        String orphanKey3 = prefix + "uploaded/orphan-3.txt";

        when(storagePort.listObjects(prefix, null, 100))
                .thenReturn(new StorageObjectListPage(List.of(orphanKey1, orphanKey2, orphanKey3), null));
        when(fileRepository.findByFileKeyIn(List.of(orphanKey1, orphanKey2, orphanKey3)))
                .thenReturn(List.of());
        when(storagePort.deleteObjects(List.of(orphanKey1, orphanKey2)))
                .thenReturn(new StorageDeleteResult(List.of(orphanKey1, orphanKey2), List.of()));

        CleanupOrphanObjectsOutput output = fileCleanupService.cleanupOrphanObjects(new CleanupOrphanObjectsInput(
                orgId,
                100,
                20,
                2,
                Duration.ZERO
        ));

        assertEquals(2, output.deletedCount());
        assertEquals(1, output.scannedPageCount());
        assertEquals(3, output.scannedObjectCount());
        assertEquals(true, output.stoppedByDeleteLimit());
        verify(storagePort).deleteObjects(List.of(orphanKey1, orphanKey2));
    }
}
