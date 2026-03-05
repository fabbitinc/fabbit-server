package com.fabbitinc.server.application.team.api;

import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
}
