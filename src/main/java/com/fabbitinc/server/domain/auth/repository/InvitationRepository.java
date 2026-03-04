package com.fabbitinc.server.domain.auth.repository;

import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByOrgIdAndEmailAndStatus(UUID orgId, String email, InvitationStatus status);

    List<Invitation> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    @Modifying
    void deleteByOrgIdAndEmailAndStatus(UUID orgId, String email, InvitationStatus status);
}
