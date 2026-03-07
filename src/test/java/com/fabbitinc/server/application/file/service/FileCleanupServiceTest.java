package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(fileRepository.findByStatusAndCreatedAtBeforeAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
                eq(FileStatus.PENDING),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(first, second), List.of());
        doNothing().when(storagePort).deleteObject(any(String.class));

        int deletedCount = fileCleanupService.cleanupStalePendingFiles(Duration.ofHours(24), 100);

        assertEquals(2, deletedCount);
        verify(storagePort).deleteObject(first.getFileKey());
        verify(storagePort).deleteObject(second.getFileKey());
        verify(fileRepository).delete(first);
        verify(fileRepository).delete(second);
        verify(fileRepository, times(1)).flush();
    }

    @Test
    void cleanupExpiredDeletedFiles_스토리지_삭제가_실패해도_DB_정리는_진행한다() {
        File deleted = File.create(UUID.randomUUID(), "deleted.txt", "tenants/org/uploaded/deleted.txt", "text/plain", 30);
        deleted.markUploaded();
        deleted.softDelete();

        when(fileRepository.findByDeletedAtBeforeOrderByDeletedAtAscIdAsc(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(deleted), List.of());
        doThrow(new RuntimeException("storage error")).when(storagePort).deleteObject(deleted.getFileKey());

        int deletedCount = fileCleanupService.cleanupExpiredDeletedFiles(Duration.ofDays(7), 100);

        assertEquals(1, deletedCount);
        verify(storagePort).deleteObject(deleted.getFileKey());
        verify(fileRepository).delete(deleted);
        verify(fileRepository, times(1)).flush();
    }
}
