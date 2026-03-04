package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findFirstByUserId(UUID userId);

    @Query("""
            select m
            from Membership m
            join Organization o on m.orgId = o.id
            where m.userId = :userId and o.slug = :slug
            """)
    Optional<Membership> findByUserIdAndOrganizationSlug(UUID userId, String slug);
}
