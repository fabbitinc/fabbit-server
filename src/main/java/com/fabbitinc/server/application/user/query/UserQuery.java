package com.fabbitinc.server.application.user.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.user.query.condition.MeCondition;
import com.fabbitinc.server.application.user.query.result.MeResult;
import com.fabbitinc.server.application.user.query.result.QueryOrganizationResult;
import com.fabbitinc.server.application.user.query.result.QueryUserResult;
import com.fabbitinc.server.application.user.query.result.UserMembershipResult;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserRepository userRepository;
    private final OrganizationApi organizationApi;
    private final SubscriptionApi subscriptionApi;
    private final FileUrlResolver fileUrlResolver;

    public MeResult getMe(MeCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        List<UserMembershipResult> membershipResults = organizationApi.getMembershipsByUserId(user.getId()).stream()
                .map(this::toMembershipResponse)
                .toList();

        return new MeResult(toUserResult(user), membershipResults);
    }

    private UserMembershipResult toMembershipResponse(Membership membership) {
        Organization organization = organizationApi.getOrganizationOrThrow(membership.getOrgId());

        return new UserMembershipResult(
                membership.getOrgId(),
                membership.getRole(),
                membership.getJobRole(),
                new QueryOrganizationResult(
                        organization.getId(),
                        organization.getSlug(),
                        organization.getName(),
                        organization.getIndustry(),
                        organization.getTeamSize(),
                        subscriptionApi.getCurrentPlanType(organization.getId()),
                        fileUrlResolver.resolve(organization.getProfileImageFileKey())
                )
        );
    }

    private QueryUserResult toUserResult(User user) {
        return new QueryUserResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
