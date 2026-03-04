package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.dto.response.ManageTeamMembersResponse;
import com.fabbitinc.server.application.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RemoveTeamMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    @Transactional
    public ManageTeamMembersResponse execute(UUID teamId, List<UUID> userIds) {
        currentAuthProvider.getCurrentAuth();
        int count = teamService.removeMembers(teamId, userIds);
        return new ManageTeamMembersResponse(count);
    }
}
