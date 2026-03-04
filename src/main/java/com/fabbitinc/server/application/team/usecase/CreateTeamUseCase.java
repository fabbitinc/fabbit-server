package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import com.fabbitinc.server.domain.team.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateTeamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    @Transactional
    public java.util.UUID execute(String name, String description) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Team team = teamService.createTeam(auth.userId(), name, description);
        return team.getId();
    }
}
