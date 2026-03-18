package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.service.input.UpgradeStarterSubscriptionInput;
import com.fabbitinc.server.application.subscription.usecase.command.UpgradeStarterSubscriptionCommand;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpgradeStarterSubscriptionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;
    private final SubscriptionApi subscriptionApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UpgradeStarterSubscriptionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        if (command.memberSeats() == null || command.memberSeats().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "현재 워크스페이스 멤버 전원의 좌석 타입을 지정해야 합니다");
        }
        WorkspacePlanType targetPlanType = command.targetPlanType();
        if (targetPlanType == WorkspacePlanType.ENTERPRISE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Enterprise 플랜 업그레이드는 별도 문의로만 지원합니다");
        }

        List<Membership> memberships = organizationApi.getMembershipsOrdered(auth.orgId());
        Map<java.util.UUID, Membership> membershipsById = memberships.stream()
                .collect(Collectors.toMap(Membership::getId, Function.identity()));
        Map<java.util.UUID, UpgradeStarterSubscriptionCommand.MemberSeatCommand> requestedSeatsByMembershipId = command.memberSeats()
                .stream()
                .collect(Collectors.toMap(
                        UpgradeStarterSubscriptionCommand.MemberSeatCommand::membershipId,
                        Function.identity(),
                        (left, right) -> {
                            throw new AppException(ErrorCode.VALIDATION_ERROR, "중복된 멤버 좌석 정보가 있습니다");
                        }
                ));

        if (memberships.size() != requestedSeatsByMembershipId.size()
                || !requestedSeatsByMembershipId.keySet().containsAll(membershipsById.keySet())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "현재 워크스페이스 멤버 전원의 좌석 타입을 지정해야 합니다");
        }

        List<UpgradeStarterSubscriptionInput.MemberSeatSelection> memberSeatSelections = memberships.stream()
                .map(membership -> new UpgradeStarterSubscriptionInput.MemberSeatSelection(
                        membership,
                        requestedSeatsByMembershipId.get(membership.getId()).seatType()
                ))
                .toList();

        subscriptionApi.upgradeStarterSubscription(new UpgradeStarterSubscriptionInput(
                auth.orgId(),
                targetPlanType,
                memberSeatSelections,
                auth.userId()
        ));

        log.atInfo()
                .addKeyValue("event.name", "subscription.starter.upgraded")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("requested.planType", targetPlanType)
                .addKeyValue("member.count", memberSeatSelections.size())
                .addKeyValue("outcome", "success")
                .log("starter subscription upgraded");
    }
}
