package com.fabbitinc.server.presentation.team.controller;

import com.fabbitinc.server.application.team.dto.request.CreateTeamRequest;
import com.fabbitinc.server.application.team.dto.request.UpdateTeamRequest;
import com.fabbitinc.server.application.team.query.TeamQuery;
import com.fabbitinc.server.application.team.query.condition.TeamDetailCondition;
import com.fabbitinc.server.application.team.query.condition.TeamLookupCondition;
import com.fabbitinc.server.application.team.query.result.TeamDetailResult;
import com.fabbitinc.server.application.team.query.result.TeamListResult;
import com.fabbitinc.server.application.team.query.result.TeamLookupResult;
import com.fabbitinc.server.application.team.usecase.CreateTeamUseCase;
import com.fabbitinc.server.application.team.usecase.DeleteTeamUseCase;
import com.fabbitinc.server.application.team.usecase.UpdateTeamUseCase;
import com.fabbitinc.server.application.team.usecase.command.CreateTeamCommand;
import com.fabbitinc.server.application.team.usecase.command.DeleteTeamCommand;
import com.fabbitinc.server.application.team.usecase.command.UpdateTeamCommand;
import com.fabbitinc.server.application.team.usecase.result.CreateTeamResult;
import com.fabbitinc.server.application.team.usecase.result.UpdateTeamResult;
import com.fabbitinc.server.presentation.team.dto.response.TeamDetailResponse;
import com.fabbitinc.server.presentation.team.dto.response.TeamListResponse;
import com.fabbitinc.server.presentation.team.dto.response.TeamLookupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
@Tag(name = "teams", description = "팀 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class TeamController {

    private final TeamQuery teamQuery;
    private final CreateTeamUseCase createTeamUseCase;
    private final UpdateTeamUseCase updateTeamUseCase;
    private final DeleteTeamUseCase deleteTeamUseCase;

    @Operation(
            summary = "POST /api/v1/teams",
            description = "팀을 생성합니다. 생성 직후 팀 상세를 반환합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDetailResponse createTeam(
            @Parameter(description = "팀 생성 요청")
            @Valid @RequestBody CreateTeamRequest request
    ) {
        CreateTeamResult result = createTeamUseCase.execute(
                new CreateTeamCommand(request.name(), request.description())
        );
        return toTeamDetailResponse(teamQuery.get(new TeamDetailCondition(result.teamId())));
    }

    @Operation(
            summary = "GET /api/v1/teams/lookup",
            description = "팀 picker/autocomplete용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public TeamLookupResponse lookupTeams(
            @Parameter(description = "팀 이름 검색어", example = "플랫폼")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toTeamLookupResponse(teamQuery.lookup(new TeamLookupCondition(search, limit)));
    }

    @Operation(
            summary = "GET /api/v1/teams",
            description = "팀 목록을 조회합니다"
    )
    @GetMapping
    public TeamListResponse listTeams() {
        return toTeamListResponse(teamQuery.list());
    }

    @Operation(
            summary = "GET /api/v1/teams/{teamId}",
            description = "팀 상세를 조회합니다"
    )
    @GetMapping("/{teamId}")
    public TeamDetailResponse getTeam(
            @Parameter(description = "조회할 팀 ID")
            @PathVariable UUID teamId
    ) {
        return toTeamDetailResponse(teamQuery.get(new TeamDetailCondition(teamId)));
    }

    @Operation(
            summary = "PATCH /api/v1/teams/{teamId}",
            description = "팀 이름/설명을 부분 수정하고 최신 팀 상세를 반환합니다"
    )
    @PatchMapping("/{teamId}")
    public TeamDetailResponse updateTeam(
            @Parameter(description = "수정할 팀 ID")
            @PathVariable UUID teamId,
            @Parameter(description = "팀 수정 요청")
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        UpdateTeamResult result = updateTeamUseCase.execute(
                new UpdateTeamCommand(teamId, request.name(), request.description())
        );
        return toTeamDetailResponse(teamQuery.get(new TeamDetailCondition(result.teamId())));
    }

    @Operation(
            summary = "DELETE /api/v1/teams/{teamId}",
            description = "팀을 삭제합니다. 팀 멤버 관계도 함께 제거됩니다"
    )
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "삭제할 팀 ID")
            @PathVariable UUID teamId
    ) {
        deleteTeamUseCase.execute(new DeleteTeamCommand(teamId));
        return ResponseEntity.noContent().build();
    }

    private TeamDetailResponse toTeamDetailResponse(TeamDetailResult result) {
        return new TeamDetailResponse(
                result.id(),
                result.name(),
                result.description(),
                result.memberCount(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private TeamLookupResponse toTeamLookupResponse(TeamLookupResult result) {
        return new TeamLookupResponse(
                result.items().stream()
                        .map(item -> new TeamLookupResponse.TeamLookupItemResponse(
                                item.id(),
                                item.name()
                        ))
                        .toList()
        );
    }

    private TeamListResponse toTeamListResponse(TeamListResult result) {
        return new TeamListResponse(
                result.items().stream()
                        .map(item -> new TeamListResponse.TeamSummaryItemResponse(
                                item.id(),
                                item.name(),
                                item.description(),
                                item.memberCount(),
                                item.createdBy(),
                                item.createdAt()
                        ))
                        .toList()
        );
    }
}
