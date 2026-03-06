package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import com.fabbitinc.server.application.team.usecase.command.CreateTeamCommand;
import com.fabbitinc.server.application.team.usecase.result.CreateTeamResult;
import com.fabbitinc.server.domain.team.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateTeamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    public CreateTeamResult execute(CreateTeamCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Team team = teamService.createTeam(auth.userId(), command.name(), command.description());
        return new CreateTeamResult(team.getId());
    }
}
