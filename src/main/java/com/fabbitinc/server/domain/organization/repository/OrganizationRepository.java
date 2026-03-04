package com.fabbitinc.server.domain.organization.repository;

import com.fabbitinc.server.domain.organization.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
