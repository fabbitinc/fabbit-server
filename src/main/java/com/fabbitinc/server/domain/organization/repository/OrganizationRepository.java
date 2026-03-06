package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Organization;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select o from Organization o where o.id = :orgId")
    Optional<Organization> findByIdForUpdate(UUID orgId);

    @Modifying
    @Query("""
            update Organization o
            set o.usedMembers = o.usedMembers + 1
            where o.id = :orgId
              and (o.maxMembers = -1 or o.usedMembers < o.maxMembers)
            """)
    int reserveMemberSeat(UUID orgId);

    @Modifying
    @Query("""
            update Organization o
            set o.usedMembers = case
                when o.usedMembers > 0 then o.usedMembers - 1
                else 0
            end
            where o.id = :orgId
            """)
    int releaseMemberSeat(UUID orgId);
}
