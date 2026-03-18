package com.fabbitinc.server.application.auth.api;

import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.auth.repository.InvitationRepository;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthInvitationApi {

    private final InvitationRepository invitationRepository;

    public long countPendingInvitations(UUID orgId, SeatType seatType) {
        return invitationRepository.countByOrgIdAndSeatTypeAndStatus(orgId, seatType, InvitationStatus.PENDING);
    }

    public Map<SeatType, Integer> countPendingInvitationsBySeatType(UUID orgId) {
        return Arrays.stream(SeatType.values())
                .collect(Collectors.toMap(
                        seatType -> seatType,
                        seatType -> Math.toIntExact(countPendingInvitations(orgId, seatType))
                ));
    }
}
