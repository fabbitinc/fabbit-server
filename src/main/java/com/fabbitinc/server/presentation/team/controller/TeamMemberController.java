package com.fabbitinc.server.presentation.team.controller;

import com.fabbitinc.server.application.team.query.TeamQuery;
import com.fabbitinc.server.application.team.query.condition.TeamMemberListCondition;
import com.fabbitinc.server.application.team.query.result.TeamMemberListResult;
import com.fabbitinc.server.application.team.usecase.AddTeamMembersUseCase;
import com.fabbitinc.server.application.team.usecase.RemoveTeamMembersUseCase;
import com.fabbitinc.server.application.team.usecase.command.AddTeamMembersCommand;
import com.fabbitinc.server.application.team.usecase.command.RemoveTeamMembersCommand;
import com.fabbitinc.server.application.team.usecase.result.AddTeamMembersResult;
import com.fabbitinc.server.presentation.team.dto.request.AddTeamMembersRequest;
import com.fabbitinc.server.presentation.team.dto.request.RemoveTeamMembersRequest;
import com.fabbitinc.server.presentation.team.dto.response.ManageTeamMembersResponse;
import com.fabbitinc.server.presentation.team.dto.response.TeamMemberListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/members")
@Tag(name = "team-members", description = "팀 멤버 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "추가 성공"),
        @ApiResponse(responseCode = "204", description = "제거 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class TeamMemberController {

    private final TeamQuery teamQuery;
    private final AddTeamMembersUseCase addTeamMembersUseCase;
    private final RemoveTeamMembersUseCase removeTeamMembersUseCase;

    @Operation(
            summary = "팀 멤버 목록을 조회합니다",
            description = "팀 멤버 목록을 조회합니다"
    )
    @GetMapping
    public TeamMemberListResponse listTeamMembers(
            @Parameter(description = "멤버를 조회할 팀 ID")
            @PathVariable UUID teamId
    ) {
        return toTeamMemberListResponse(teamQuery.listMembers(new TeamMemberListCondition(teamId)));
    }

    @Operation(
            summary = "팀에 멤버를 배치 추가합니다. 이미 추가된 멤버는 제외됩니다",
            description = "팀에 멤버를 배치 추가합니다. 이미 추가된 멤버는 제외됩니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManageTeamMembersResponse addTeamMembers(
            @Parameter(description = "멤버를 추가할 팀 ID")
            @PathVariable UUID teamId,
            @Parameter(description = "팀 멤버 추가 요청")
            @Valid @RequestBody AddTeamMembersRequest request
    ) {
        AddTeamMembersResult result = addTeamMembersUseCase.execute(
                new AddTeamMembersCommand(teamId, request.userIds())
        );
        return new ManageTeamMembersResponse(result.count());
    }

    @Operation(
            summary = "팀에서 멤버를 배치 제거합니다",
            description = "팀에서 멤버를 배치 제거합니다"
    )
    @DeleteMapping
    public ResponseEntity<Void> removeTeamMembers(
            @Parameter(description = "멤버를 제거할 팀 ID")
            @PathVariable UUID teamId,
            @Parameter(description = "팀 멤버 제거 요청")
            @Valid @RequestBody RemoveTeamMembersRequest request
    ) {
        removeTeamMembersUseCase.execute(new RemoveTeamMembersCommand(teamId, request.userIds()));
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
