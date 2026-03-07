package com.fabbitinc.server.application.team.api;

import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamApi {

    private final TeamRepository teamRepository;

    public Team getTeamOrNull(UUID teamId) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(teamId).orElse(null);
    }

    public List<Team> getTeamsByIds(List<UUID> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }
        return teamRepository.findAllById(teamIds);
    }
}
