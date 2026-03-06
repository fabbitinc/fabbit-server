package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.api.TeamApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.query.condition.PartDefaultOwnerCondition;
import com.fabbitinc.server.application.part.query.condition.PartOwnerCondition;
import com.fabbitinc.server.application.part.query.result.PartDefaultOwnerListResult;
import com.fabbitinc.server.application.part.query.result.PartOwnerResult;
import com.fabbitinc.server.application.part.query.result.PartUserSummaryResult;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartOwnerQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRepository partRepository;
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final UserApi userApi;
    private final TeamApi teamApi;
    private final FileUrlResolver fileUrlResolver;

    public PartOwnerResult get(PartOwnerCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Part part = partRepository.findById(condition.partId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + condition.partId() + "'을(를) 찾을 수 없습니다"
                ));
        return toPartOwnerResult(part);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PartDefaultOwnerListResult listDefaultOwners() {
        currentAuthProvider.getCurrentAuth();

        List<PartDefaultOwner> rows = partDefaultOwnerRepository.findAllOrderByCategoryNullsFirst();
        Map<UUID, User> usersById = loadUsersById(rows.stream()
                .map(PartDefaultOwner::getDefaultOwnerId)
                .filter(java.util.Objects::nonNull)
                .toList());
        Map<UUID, Team> teamsById = loadTeamsById(rows.stream()
                .map(PartDefaultOwner::getDefaultOwnerTeamId)
                .filter(java.util.Objects::nonNull)
                .toList());

        List<PartDefaultOwnerListResult.Item> items = rows.stream()
                .map(row -> toPartDefaultOwnerItemResult(row, usersById, teamsById))
                .toList();
        return new PartDefaultOwnerListResult(items);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PartDefaultOwnerListResult.Item get(PartDefaultOwnerCondition condition) {
        currentAuthProvider.getCurrentAuth();

        PartDefaultOwner row = partDefaultOwnerRepository.findById(condition.defaultOwnerId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "기본 담당자 설정을 찾을 수 없습니다"
                ));
        return toPartDefaultOwnerItemResult(
                row,
                loadUsersById(row.getDefaultOwnerId() == null ? List.of() : List.of(row.getDefaultOwnerId())),
                loadTeamsById(row.getDefaultOwnerTeamId() == null ? List.of() : List.of(row.getDefaultOwnerTeamId()))
        );
    }

    private PartOwnerResult toPartOwnerResult(Part part) {
        User owner = userApi.getUserOrNull(part.getOwnerId());
        Team ownerTeam = teamApi.getTeamOrNull(part.getOwnerTeamId());
        return new PartOwnerResult(
                part.getOwnerId(),
                toUserSummary(owner),
                part.getOwnerTeamId(),
                toTeamName(ownerTeam)
        );
    }

    private PartDefaultOwnerListResult.Item toPartDefaultOwnerItemResult(
            PartDefaultOwner row,
            Map<UUID, User> usersById,
            Map<UUID, Team> teamsById
    ) {
        return new PartDefaultOwnerListResult.Item(
                row.getId(),
                row.getCategory(),
                row.getDefaultOwnerId(),
                toUserSummary(usersById.get(row.getDefaultOwnerId())),
                row.getDefaultOwnerTeamId(),
                toTeamName(teamsById.get(row.getDefaultOwnerTeamId()))
        );
    }

    private Map<UUID, User> loadUsersById(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userApi.getUsersByIdsOrdered(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<UUID, Team> loadTeamsById(List<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return teamApi.getTeamsByIds(teamIds).stream()
                .collect(java.util.stream.Collectors.toMap(Team::getId, team -> team, (left, right) -> left, LinkedHashMap::new));
    }

    private PartUserSummaryResult toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new PartUserSummaryResult(
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
