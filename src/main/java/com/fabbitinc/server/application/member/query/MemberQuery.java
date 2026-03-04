package com.fabbitinc.server.application.member.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.member.dto.response.MemberListResponse;
import com.fabbitinc.server.application.member.dto.response.MemberLookupItemResponse;
import com.fabbitinc.server.application.member.dto.response.MemberLookupResponse;
import com.fabbitinc.server.application.member.dto.response.MemberSummaryResponse;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
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
public class MemberQuery {

    private final AuthTokenParser authTokenParser;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional(readOnly = true)
    public MemberLookupResponse lookupMembers(String authorizationHeader, String search, int limit) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        List<Membership> memberships = membershipRepository.findOrderedByOrgId(auth.orgId());
        List<UUID> userIds = memberships.stream().map(Membership::getUserId).toList();
        Map<UUID, User> users = userRepository.findAllByIdInOrderByFullName(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        String normalizedSearch = search == null ? null : search.toLowerCase(Locale.ROOT);

        List<MemberLookupItemResponse> items = memberships.stream()
                .map(Membership::getUserId)
                .map(users::get)
                .filter(user -> user != null)
                .filter(user -> normalizedSearch == null || user.getFullName().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .limit(limit)
                .map(this::toLookupItem)
                .toList();

        return new MemberLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public MemberListResponse listOrgMembers(String authorizationHeader) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        Organization organization = organizationRepository.findById(auth.orgId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));

        List<Membership> memberships = membershipRepository.findOrderedByOrgId(auth.orgId());
        List<UUID> userIds = memberships.stream().map(Membership::getUserId).toList();
        Map<UUID, User> users = userRepository.findAllByIdInOrderByFullName(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MemberSummaryResponse> items = memberships.stream()
                .map(membership -> toMemberSummary(membership, users.get(membership.getUserId())))
                .filter(item -> item != null)
                .sorted(Comparator
                        .comparing((MemberSummaryResponse item) -> roleOrder(MembershipRole.from(item.role())))
                        .thenComparing(MemberSummaryResponse::fullName))
                .toList();

        return new MemberListResponse(items, organization.getMaxMembers());
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

    private MemberLookupItemResponse toLookupItem(User user) {
        return new MemberLookupItemResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private MemberSummaryResponse toMemberSummary(Membership membership, User user) {
        if (user == null) {
            return null;
        }

        return new MemberSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                membership.getRole().name(),
                membership.getJobRole()
        );
    }
}
