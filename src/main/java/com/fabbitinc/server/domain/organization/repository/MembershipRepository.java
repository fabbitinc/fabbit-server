package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findFirstByUserId(UUID userId);

    List<Membership> findByUserId(UUID userId);

    Optional<Membership> findByUserIdAndOrgId(UUID userId, UUID orgId);

    long countByOrgId(UUID orgId);

    long countByOrgIdAndRole(UUID orgId, MembershipRole role);

    void deleteByOrgIdAndUserId(UUID orgId, UUID userId);

    @Query("""
            select m
            from Membership m
            join Organization o on m.orgId = o.id
            where m.userId = :userId and o.slug = :slug
            """)
    Optional<Membership> findByUserIdAndOrganizationSlug(UUID userId, String slug);

    @Query("""
            select m
            from Membership m
            where m.orgId = :orgId
            order by case
                when m.role = com.fabbitinc.server.domain.organization.model.MembershipRole.OWNER then 0
                when m.role = com.fabbitinc.server.domain.organization.model.MembershipRole.ADMIN then 1
                else 2
            end, m.userId
            """)
    List<Membership> findOrderedByOrgId(UUID orgId);
}
