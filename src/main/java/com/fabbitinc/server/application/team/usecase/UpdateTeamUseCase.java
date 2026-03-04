package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateTeamUseCase {

    private final AuthTokenParser authTokenParser;
    private final TeamService teamService;

    @Transactional
    public UUID execute(String authorizationHeader, UUID teamId, String name, String description) {
        authTokenParser.requireAuth(authorizationHeader);
        return teamService.updateTeam(teamId, name, description).getId();
    }
}
