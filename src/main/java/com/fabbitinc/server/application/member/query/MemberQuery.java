package com.fabbitinc.server.application.member.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.member.query.condition.MemberListCondition;
import com.fabbitinc.server.application.member.query.condition.MemberLookupCondition;
import com.fabbitinc.server.application.member.query.result.MemberListResult;
import com.fabbitinc.server.application.member.query.result.MemberLookupItemResult;
import com.fabbitinc.server.application.member.query.result.MemberLookupResult;
import com.fabbitinc.server.application.member.query.result.MemberSummaryResult;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;
    private final UserApi userApi;
    private final FileUrlResolver fileUrlResolver;

    public MemberLookupResult lookup(MemberLookupCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<Membership> memberships = organizationApi.getMembershipsOrdered(auth.orgId());
        List<UUID> userIds = memberships.stream().map(Membership::getUserId).toList();
        Map<UUID, User> users = userApi.getUsersByIdsOrdered(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        String normalizedSearch = condition.search() == null ? null : condition.search().toLowerCase(Locale.ROOT);

        List<MemberLookupItemResult> items = memberships.stream()
                .map(Membership::getUserId)
                .map(users::get)
                .filter(user -> user != null)
                .filter(user -> normalizedSearch == null || user.getFullName().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .limit(condition.limit())
                .map(this::toLookupItem)
                .toList();

        return new MemberLookupResult(items);
    }

    public MemberListResult list(MemberListCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        Organization organization = organizationApi.getOrganizationOrThrow(auth.orgId());

        List<Membership> memberships = organizationApi.getMembershipsOrdered(auth.orgId());
        List<UUID> userIds = memberships.stream().map(Membership::getUserId).toList();
        Map<UUID, User> users = userApi.getUsersByIdsOrdered(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MemberSummaryResult> items = memberships.stream()
                .map(membership -> toMemberSummary(membership, users.get(membership.getUserId())))
                .filter(item -> item != null)
                .sorted(Comparator
                        .comparing((MemberSummaryResult item) -> roleOrder(item.role()))
                        .thenComparing(MemberSummaryResult::fullName))
                .toList();

        return new MemberListResult(items, organization.getMaxMembers());
    }

    private int roleOrder(MembershipRole role) {
        if (role == MembershipRole.OWNER) {
            return 0;
        }
        if (role == MembershipRole.ADMIN) {
            return 1;
        }
        return 2;
    }

    private MemberLookupItemResult toLookupItem(User user) {
        return new MemberLookupItemResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private MemberSummaryResult toMemberSummary(Membership membership, User user) {
        if (user == null) {
            return null;
        }

        return new MemberSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                membership.getRole(),
                membership.getJobRole()
        );
    }
}
