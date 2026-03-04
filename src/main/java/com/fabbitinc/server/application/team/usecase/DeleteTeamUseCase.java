package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteTeamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    @Transactional
    public void execute(UUID teamId) {
        currentAuthProvider.getCurrentAuth();
        teamService.deleteTeam(teamId);
    }
}
