package com.fabbitinc.server.application.user.query;

import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.user.dto.response.MeResponse;
import com.fabbitinc.server.application.user.dto.response.UserMembershipResponse;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserQuery {

    private final AuthTokenParser authTokenParser;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional(readOnly = true)
    public MeResponse getMe(String authorizationHeader) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        List<UserMembershipResponse> membershipResponses = membershipRepository.findByUserId(user.getId()).stream()
                .map(this::toMembershipResponse)
                .toList();

        return new MeResponse(toUserResponse(user), membershipResponses);
    }

    private UserMembershipResponse toMembershipResponse(Membership membership) {
        Organization organization = organizationRepository.findById(membership.getOrgId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));

        return new UserMembershipResponse(
                membership.getOrgId(),
                membership.getRole().name(),
                membership.getJobRole(),
                new OrganizationResponse(
                        organization.getId(),
                        organization.getSlug(),
                        organization.getName(),
                        organization.getIndustry(),
                        organization.getTeamSize(),
                        organization.getPlanType().name(),
                        fileUrlResolver.resolve(organization.getProfileImageFileKey())
                )
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
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
