package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findFirstByUserId(UUID userId);

    List<Membership> findByUserId(UUID userId);

    Optional<Membership> findByUserIdAndOrgId(UUID userId, UUID orgId);

    List<Membership> findByOrgId(UUID orgId);

    long countByOrgId(UUID orgId);

    long countByOrgIdAndRole(UUID orgId, MembershipRole role);

    void deleteByOrgIdAndUserId(UUID orgId, UUID userId);
}
