package com.fabbitinc.server.presentation.team.controller;

import com.fabbitinc.server.application.team.dto.request.AddTeamMembersRequest;
import com.fabbitinc.server.application.team.dto.request.RemoveTeamMembersRequest;
import com.fabbitinc.server.application.team.dto.response.ManageTeamMembersResponse;
import com.fabbitinc.server.application.team.query.condition.TeamMemberListCondition;
import com.fabbitinc.server.application.team.query.result.TeamMemberListResult;
import com.fabbitinc.server.application.team.query.TeamQuery;
import com.fabbitinc.server.application.team.usecase.AddTeamMembersUseCase;
import com.fabbitinc.server.application.team.usecase.RemoveTeamMembersUseCase;
import com.fabbitinc.server.presentation.team.dto.response.TeamMemberListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/members")
@Tag(name = "team-members", description = "팀 멤버 관리 API")
public class TeamMemberController {

    private final TeamQuery teamQuery;
    private final AddTeamMembersUseCase addTeamMembersUseCase;
    private final RemoveTeamMembersUseCase removeTeamMembersUseCase;

    @Operation(
            summary = "GET /api/v1/teams/{teamId}/members",
            description = "팀 멤버 목록을 조회합니다"
    )
    @GetMapping
    public TeamMemberListResponse listTeamMembers(
            @PathVariable UUID teamId
    ) {
        return toTeamMemberListResponse(teamQuery.listMembers(new TeamMemberListCondition(teamId)));
    }

    @Operation(
            summary = "POST /api/v1/teams/{teamId}/members",
            description = "팀에 멤버를 배치 추가합니다. 이미 추가된 멤버는 제외됩니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManageTeamMembersResponse addTeamMembers(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddTeamMembersRequest request
    ) {
        return addTeamMembersUseCase.execute(teamId, request.userIds());
    }

    @Operation(
            summary = "DELETE /api/v1/teams/{teamId}/members",
            description = "팀에서 멤버를 배치 제거합니다"
    )
    @DeleteMapping
    public ResponseEntity<Void> removeTeamMembers(
            @PathVariable UUID teamId,
            @Valid @RequestBody RemoveTeamMembersRequest request
    ) {
        removeTeamMembersUseCase.execute(teamId, request.userIds());
        return ResponseEntity.noContent().build();
    }

    private TeamMemberListResponse toTeamMemberListResponse(TeamMemberListResult result) {
        return new TeamMemberListResponse(
                result.items().stream()
                        .map(item -> new TeamMemberListResponse.TeamMemberItemResponse(
                                item.userId(),
                                item.fullName(),
                                item.email(),
                                item.phone(),
                                item.profileImageUrl()
                        ))
                        .toList()
        );
    }
}
