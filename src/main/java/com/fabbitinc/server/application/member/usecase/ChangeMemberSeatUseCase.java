package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.member.usecase.command.ChangeMemberSeatCommand;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.domain.organization.model.Membership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChangeMemberSeatUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;
    private final SubscriptionApi subscriptionApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(ChangeMemberSeatCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Membership membership = organizationApi.getMembershipOrThrow(command.userId(), auth.orgId());
        subscriptionApi.changeSeatType(auth.orgId(), membership, command.seatType(), auth.userId());
        log.atInfo()
                .addKeyValue("event.name", "member.seat.changed")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("target.user.id", command.userId())
                .addKeyValue("new.seatType", command.seatType())
                .addKeyValue("outcome", "success")
                .log("member seat changed");
    }
}
