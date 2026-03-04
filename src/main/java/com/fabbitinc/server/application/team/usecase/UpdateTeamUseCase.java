package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateTeamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    @Transactional
    public UUID execute(UUID teamId, String name, String description) {
        currentAuthProvider.getCurrentAuth();
        return teamService.updateTeam(teamId, name, description).getId();
    }
}
