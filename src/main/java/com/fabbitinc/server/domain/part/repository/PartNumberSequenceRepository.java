package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartNumberSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PartNumberSequenceRepository extends JpaRepository<PartNumberSequence, UUID> {

    Optional<PartNumberSequence> findByCategoryId(UUID categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PartNumberSequence s where s.categoryId = :categoryId")
    Optional<PartNumberSequence> findByCategoryIdForUpdate(UUID categoryId);
}
