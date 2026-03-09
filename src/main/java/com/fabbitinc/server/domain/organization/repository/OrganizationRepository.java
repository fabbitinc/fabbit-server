package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Organization;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByOwnerId(UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select o from Organization o where o.id = :orgId")
    Optional<Organization> findByIdForUpdate(UUID orgId);

    @Modifying
    @Transactional
    @Query("""
            update Organization o
            set o.usedMembers = o.usedMembers + 1
            where o.id = :orgId
              and (o.maxMembers = -1 or o.usedMembers < o.maxMembers)
            """)
    int reserveMemberSeat(UUID orgId);

    @Modifying
    @Transactional
    @Query("""
            update Organization o
            set o.usedMembers = case
                when o.usedMembers > 0 then o.usedMembers - 1
                else 0
            end
            where o.id = :orgId
            """)
    int releaseMemberSeat(UUID orgId);

    @Modifying
    @Transactional
    @Query("""
            update Organization o
            set o.storageBytesUsed = o.storageBytesUsed + :deltaBytes
            where o.id = :orgId
              and (o.allowStorageOverage = true or o.storageBytesUsed + :deltaBytes <= o.storageBytesLimit)
            """)
    int consumeStorageBytes(UUID orgId, long deltaBytes);

    @Modifying
    @Transactional
    @Query("""
            update Organization o
            set o.storageBytesUsed = case
                when o.storageBytesUsed > :deltaBytes then o.storageBytesUsed - :deltaBytes
                else 0
            end
            where o.id = :orgId
            """)
    int releaseStorageBytes(UUID orgId, long deltaBytes);
}
