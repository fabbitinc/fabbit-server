package com.fabbitinc.server.application.team.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public Team createTeam(UUID actorId, String name, String description) {
        Team team = Team.create(name, description, actorId);
        return teamRepository.save(team);
    }

    public Team updateTeam(UUID actorId, UUID teamId, String name, String description) {
        Team team = getOrThrow(teamId);
        if (name != null && !name.equals(team.getName())) {
            team.changeName(name, actorId);
        }
        if (description != null && !description.equals(team.getDescription())) {
            team.changeDescription(description, actorId);
        }
        return team;
    }

    public void deleteTeam(UUID teamId) {
        Team team = getOrThrow(teamId);
        teamRepository.delete(team);
    }

    public int addMembers(UUID teamId, List<UUID> userIds) {
        Team team = getOrThrow(teamId);

        List<UUID> normalizedUserIds = List.copyOf(new LinkedHashSet<>(userIds));
        Set<UUID> existingUserIds = teamMemberRepository.findByTeamAndUserIdIn(team, normalizedUserIds).stream()
                .map(TeamMember::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        List<TeamMember> newMembers = normalizedUserIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(team::addMember)
                .toList();

        if (newMembers.isEmpty()) {
            return 0;
        }

        teamMemberRepository.saveAll(newMembers);
        return newMembers.size();
    }

    public int removeMembers(UUID teamId, List<UUID> userIds) {
        Team team = getOrThrow(teamId);
        List<UUID> normalizedUserIds = List.copyOf(new LinkedHashSet<>(userIds));
        return teamMemberRepository.deleteByTeamAndUserIdIn(team, normalizedUserIds);
    }

    public Team getOrThrow(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Team '" + teamId + "'을(를) 찾을 수 없습니다"
                ));
    }
}
