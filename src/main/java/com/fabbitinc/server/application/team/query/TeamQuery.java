package com.fabbitinc.server.application.team.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.team.dto.response.TeamDetailResponse;
import com.fabbitinc.server.application.team.dto.response.TeamListResponse;
import com.fabbitinc.server.application.team.dto.response.TeamLookupItemResponse;
import com.fabbitinc.server.application.team.dto.response.TeamLookupResponse;
import com.fabbitinc.server.application.team.dto.response.TeamMemberListResponse;
import com.fabbitinc.server.application.team.dto.response.TeamMemberSummaryResponse;
import com.fabbitinc.server.application.team.dto.response.TeamSummaryResponse;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TeamQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional(readOnly = true)
    public TeamListResponse listTeams() {
        currentAuthProvider.getCurrentAuth();

        List<TeamSummaryResponse> items = teamRepository.findAllByOrderByNameAsc().stream()
                .map(this::toTeamSummaryResponse)
                .toList();
        return new TeamListResponse(items);
    }

    @Transactional(readOnly = true)
    public TeamLookupResponse lookupTeams(String search, int limit) {
        currentAuthProvider.getCurrentAuth();

        List<Team> teams;
        if (search == null || search.isBlank()) {
            teams = teamRepository.findAllByOrderByNameAsc(PageRequest.of(0, limit));
        } else {
            teams = teamRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), PageRequest.of(0, limit));
        }
        List<TeamLookupItemResponse> items = teams.stream()
                .map(team -> new TeamLookupItemResponse(team.getId(), team.getName()))
                .toList();
        return new TeamLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(UUID teamId) {
        currentAuthProvider.getCurrentAuth();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Team '" + teamId + "'을(를) 찾을 수 없습니다"
                ));

        return toTeamDetailResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamMemberListResponse listMembers(UUID teamId) {
        currentAuthProvider.getCurrentAuth();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "팀을 찾을 수 없습니다"));

        List<TeamMember> members = teamMemberRepository.findByTeam(team);
        if (members.isEmpty()) {
            return new TeamMemberListResponse(List.of());
        }

        List<UUID> userIds = members.stream().map(TeamMember::getUserId).toList();
        List<User> users = userRepository.findAllByIdInOrderByFullName(userIds);
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<TeamMemberSummaryResponse> items = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    if (user == null) {
                        return new TeamMemberSummaryResponse(member.getUserId(), "", "", null, null);
                    }
                    return new TeamMemberSummaryResponse(
                            member.getUserId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    );
                })
                .toList();

        return new TeamMemberListResponse(items);
    }

    private TeamSummaryResponse toTeamSummaryResponse(Team team) {
        int memberCount = (int) teamMemberRepository.countByTeam(team);
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                memberCount,
                team.getCreatedBy(),
                team.getCreatedAt()
        );
    }

    private TeamDetailResponse toTeamDetailResponse(Team team) {
        int memberCount = (int) teamMemberRepository.countByTeam(team);
        return new TeamDetailResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                memberCount,
                team.getCreatedBy(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
