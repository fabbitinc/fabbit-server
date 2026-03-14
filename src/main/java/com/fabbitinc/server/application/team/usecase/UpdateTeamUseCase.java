package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import com.fabbitinc.server.application.team.usecase.command.UpdateTeamCommand;
import com.fabbitinc.server.application.team.usecase.result.UpdateTeamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateTeamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    public UpdateTeamResult execute(UpdateTeamCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        return new UpdateTeamResult(
                teamService.updateTeam(auth.userId(), command.teamId(), command.name(), command.description()).getId()
        );
    }
}
