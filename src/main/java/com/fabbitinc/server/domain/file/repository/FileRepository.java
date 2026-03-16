package com.fabbitinc.server.domain.file.repository;

import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, UUID> {

    List<File> findByIdIn(Collection<UUID> fileIds);

    List<File> findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(String ownerType, UUID ownerId);

    List<File> findByOwnerTypeAndOwnerIdInAndDeletedAtIsNull(String ownerType, Collection<UUID> ownerIds);

    List<File> findByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
            String ownerType,
            UUID ownerId,
            FileStatus status
    );

    Optional<File> findByIdAndDeletedAtIsNull(UUID id);

    Optional<File> findByFileKeyAndDeletedAtIsNull(String fileKey);

    Optional<File> findByFileKey(String fileKey);

    List<File> findByFileKeyIn(Collection<String> fileKeys);

    Optional<File> findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(UUID id, String ownerType, UUID ownerId);

    long countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(String ownerType, UUID ownerId, FileStatus status);

    List<File> findByStatusAndOwnerTypeIsNotNull(FileStatus status);

    List<File> findByStatusAndCreatedAtBeforeAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
            FileStatus status,
            Instant createdAt,
            Pageable pageable
    );

    List<File> findByDeletedAtBeforeOrderByDeletedAtAscIdAsc(
            Instant deletedAt,
            Pageable pageable
    );
}
