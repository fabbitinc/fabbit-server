package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.team.dto.response.ManageTeamMembersResponse;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddTeamMembersUseCase {

    private final AuthTokenParser authTokenParser;
    private final TeamService teamService;

    @Transactional
    public ManageTeamMembersResponse execute(String authorizationHeader, UUID teamId, List<UUID> userIds) {
        authTokenParser.requireAuth(authorizationHeader);
        int count = teamService.addMembers(teamId, userIds);
        return new ManageTeamMembersResponse(count);
    }
}
