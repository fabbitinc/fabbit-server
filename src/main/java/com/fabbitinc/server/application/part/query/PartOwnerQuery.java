package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
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
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartOwnerQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRepository partRepository;
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final FileUrlResolver fileUrlResolver;

    public PartOwnerResponse getPartOwner(UUID partId) {
        currentAuthProvider.getCurrentAuth();

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));
        return toPartOwnerResponse(part);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PartDefaultOwnerListResponse listDefaultOwners() {
        currentAuthProvider.getCurrentAuth();

        List<PartDefaultOwnerItemResponse> items = partDefaultOwnerRepository.findAllOrderByCategoryNullsFirst()
                .stream()
                .map(this::toPartDefaultOwnerItemResponse)
                .toList();
        return new PartDefaultOwnerListResponse(items);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PartDefaultOwnerItemResponse getDefaultOwner(UUID defaultOwnerId) {
        currentAuthProvider.getCurrentAuth();

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
                toUserSummary(part.getOwner()),
                part.getOwnerTeamId(),
                toTeamName(part.getOwnerTeam())
        );
    }

    private PartDefaultOwnerItemResponse toPartDefaultOwnerItemResponse(PartDefaultOwner row) {
        return new PartDefaultOwnerItemResponse(
                row.getId(),
                row.getCategory(),
                row.getDefaultOwnerId(),
                toUserSummary(row.getDefaultOwner()),
                row.getDefaultOwnerTeamId(),
                toTeamName(row.getDefaultOwnerTeam())
        );
    }

    private PartOwnerUserSummaryResponse toUserSummary(User user) {
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

    private String toTeamName(Team team) {
        return team != null ? team.getName() : null;
    }
}
