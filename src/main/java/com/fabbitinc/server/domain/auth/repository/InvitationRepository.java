package com.fabbitinc.server.domain.auth.repository;

import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByOrgIdAndEmailAndStatus(UUID orgId, String email, InvitationStatus status);

    List<Invitation> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    long countByOrgIdAndSeatTypeAndStatus(UUID orgId, SeatType seatType, InvitationStatus status);

    @Modifying
    void deleteByOrgIdAndEmailAndStatus(UUID orgId, String email, InvitationStatus status);
}
