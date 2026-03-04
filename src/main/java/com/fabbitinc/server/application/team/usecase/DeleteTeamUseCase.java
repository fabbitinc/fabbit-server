package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteTeamUseCase {

    private final AuthTokenParser authTokenParser;
    private final TeamService teamService;

    @Transactional
    public void execute(String authorizationHeader, UUID teamId) {
        authTokenParser.requireAuth(authorizationHeader);
        teamService.deleteTeam(teamId);
    }
}
