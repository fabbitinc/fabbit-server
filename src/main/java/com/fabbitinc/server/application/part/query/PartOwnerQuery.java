package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerListResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerUserSummaryResponse;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartOwnerQuery {

    private final AuthTokenParser authTokenParser;
    private final PartRepository partRepository;
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional(readOnly = true)
    public PartOwnerResponse getPartOwner(String authorizationHeader, UUID partId) {
        authTokenParser.requireAuth(authorizationHeader);

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));
        return toPartOwnerResponse(part);
    }

    @Transactional(readOnly = true)
    public PartDefaultOwnerListResponse listDefaultOwners(String authorizationHeader) {
        authTokenParser.requireAdmin(authorizationHeader);

        List<PartDefaultOwnerItemResponse> items = partDefaultOwnerRepository.findAllOrderByCategoryNullsFirst()
                .stream()
                .map(this::toPartDefaultOwnerItemResponse)
                .toList();
        return new PartDefaultOwnerListResponse(items);
    }

    @Transactional(readOnly = true)
    public PartDefaultOwnerItemResponse getDefaultOwner(String authorizationHeader, UUID defaultOwnerId) {
        authTokenParser.requireAdmin(authorizationHeader);

        PartDefaultOwner row = partDefaultOwnerRepository.findById(defaultOwnerId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "기본 담당자 설정을 찾을 수 없습니다"
                ));
        return toPartDefaultOwnerItemResponse(row);
    }

    private PartOwnerResponse toPartOwnerResponse(Part part) {
        return new PartOwnerResponse(
                part.getOwnerId(),
                toUserSummary(part.getOwnerId()),
                part.getOwnerTeamId(),
                toTeamName(part.getOwnerTeamId())
        );
    }

    private PartDefaultOwnerItemResponse toPartDefaultOwnerItemResponse(PartDefaultOwner row) {
        return new PartDefaultOwnerItemResponse(
                row.getId(),
                row.getCategory(),
                row.getDefaultOwnerId(),
                toUserSummary(row.getDefaultOwnerId()),
                row.getDefaultOwnerTeamId(),
                toTeamName(row.getDefaultOwnerTeamId())
        );
    }

    private PartOwnerUserSummaryResponse toUserSummary(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        return new PartOwnerUserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private String toTeamName(UUID teamId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamRepository.findById(teamId).orElse(null);
        return team != null ? team.getName() : null;
    }
}
