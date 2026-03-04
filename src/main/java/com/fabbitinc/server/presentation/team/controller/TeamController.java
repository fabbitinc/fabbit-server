package com.fabbitinc.server.presentation.team.controller;

import com.fabbitinc.server.application.team.dto.request.CreateTeamRequest;
import com.fabbitinc.server.application.team.dto.request.UpdateTeamRequest;
import com.fabbitinc.server.application.team.dto.response.TeamDetailResponse;
import com.fabbitinc.server.application.team.dto.response.TeamListResponse;
import com.fabbitinc.server.application.team.dto.response.TeamLookupResponse;
import com.fabbitinc.server.application.team.query.TeamQuery;
import com.fabbitinc.server.application.team.usecase.CreateTeamUseCase;
import com.fabbitinc.server.application.team.usecase.DeleteTeamUseCase;
import com.fabbitinc.server.application.team.usecase.UpdateTeamUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
@Tag(name = "teams", description = "팀 관리 API")
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
            @Valid @RequestBody CreateTeamRequest request
    ) {
        UUID teamId = createTeamUseCase.execute(request.name(), request.description());
        return teamQuery.getTeamDetail(teamId);
    }

    @Operation(
            summary = "GET /api/v1/teams/lookup",
            description = "팀 picker/autocomplete용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public TeamLookupResponse lookupTeams(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return teamQuery.lookupTeams(search, limit);
    }

    @Operation(
            summary = "GET /api/v1/teams",
            description = "팀 목록을 조회합니다"
    )
    @GetMapping
    public TeamListResponse listTeams() {
        return teamQuery.listTeams();
    }

    @Operation(
            summary = "GET /api/v1/teams/{teamId}",
            description = "팀 상세를 조회합니다"
    )
    @GetMapping("/{teamId}")
    public TeamDetailResponse getTeam(
            @PathVariable UUID teamId
    ) {
        return teamQuery.getTeamDetail(teamId);
    }

    @Operation(
            summary = "PATCH /api/v1/teams/{teamId}",
            description = "팀 이름/설명을 부분 수정하고 최신 팀 상세를 반환합니다"
    )
    @PatchMapping("/{teamId}")
    public TeamDetailResponse updateTeam(
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        UUID updatedTeamId = updateTeamUseCase.execute(teamId,
                request.name(),
                request.description()
        );
        return teamQuery.getTeamDetail(updatedTeamId);
    }

    @Operation(
            summary = "DELETE /api/v1/teams/{teamId}",
            description = "팀을 삭제합니다. 팀 멤버 관계도 함께 제거됩니다"
    )
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable UUID teamId
    ) {
        deleteTeamUseCase.execute(teamId);
        return ResponseEntity.noContent().build();
    }
}
