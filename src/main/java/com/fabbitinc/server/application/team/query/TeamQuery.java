package com.fabbitinc.server.application.team.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.team.query.condition.TeamDetailCondition;
import com.fabbitinc.server.application.team.query.condition.TeamLookupCondition;
import com.fabbitinc.server.application.team.query.condition.TeamMemberListCondition;
import com.fabbitinc.server.application.team.query.result.TeamDetailResult;
import com.fabbitinc.server.application.team.query.result.TeamListResult;
import com.fabbitinc.server.application.team.query.result.TeamLookupResult;
import com.fabbitinc.server.application.team.query.result.TeamMemberListResult;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    public TeamListResult list() {
        currentAuthProvider.getCurrentAuth();

        List<TeamListResult.TeamSummaryResult> items = teamRepository.findAllByOrderByNameAsc().stream()
                .map(this::toTeamSummaryResult)
                .toList();
        return new TeamListResult(items);
    }

    public TeamLookupResult lookup(TeamLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String search = condition.search();
        int limit = condition.limit();
        List<Team> teams;
        if (search == null || search.isBlank()) {
            teams = teamRepository.findAllByOrderByNameAsc(PageRequest.of(0, limit));
        } else {
            teams = teamRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim(), PageRequest.of(0, limit));
        }
        List<TeamLookupResult.TeamLookupItemResult> items = teams.stream()
                .map(team -> new TeamLookupResult.TeamLookupItemResult(team.getId(), team.getName()))
                .toList();
        return new TeamLookupResult(items);
    }

    public TeamDetailResult get(TeamDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        UUID teamId = condition.teamId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Team '" + teamId + "'을(를) 찾을 수 없습니다"
                ));

        return toTeamDetailResult(team);
    }

    public TeamMemberListResult listMembers(TeamMemberListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        UUID teamId = condition.teamId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "팀을 찾을 수 없습니다"));

        List<TeamMember> members = teamMemberRepository.findByTeam(team);
        if (members.isEmpty()) {
            return new TeamMemberListResult(List.of());
        }

        List<UUID> userIds = members.stream().map(TeamMember::getUserId).toList();
        List<User> users = userRepository.findByIdInOrderByFullNameAsc(userIds);
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<TeamMemberListResult.TeamMemberSummaryResult> items = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    if (user == null) {
                        return new TeamMemberListResult.TeamMemberSummaryResult(
                                member.getUserId(),
                                "",
                                "",
                                null,
                                null
                        );
                    }
                    return new TeamMemberListResult.TeamMemberSummaryResult(
                            member.getUserId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    );
                })
                .toList();

        return new TeamMemberListResult(items);
    }

    private TeamListResult.TeamSummaryResult toTeamSummaryResult(Team team) {
        int memberCount = (int) teamMemberRepository.countByTeam(team);
        return new TeamListResult.TeamSummaryResult(
                team.getId(),
                team.getName(),
                team.getDescription(),
                memberCount,
                team.getCreatedBy(),
                team.getCreatedAt()
        );
    }

    private TeamDetailResult toTeamDetailResult(Team team) {
        int memberCount = (int) teamMemberRepository.countByTeam(team);
        return new TeamDetailResult(
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
