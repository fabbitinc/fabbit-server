package com.fabbitinc.server.presentation.project.controller;

import com.fabbitinc.server.presentation.project.dto.request.CreateProjectRequest;
import com.fabbitinc.server.presentation.project.dto.request.UpdateProjectRequest;
import com.fabbitinc.server.presentation.project.dto.response.ActivityListResponse;
import com.fabbitinc.server.presentation.project.dto.response.ActivityResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectDetailResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectListResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectSummaryResponse;
import com.fabbitinc.server.presentation.project.dto.response.UserSummaryResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.ProjectActivitiesCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectDetailCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectListCondition;
import com.fabbitinc.server.application.project.query.result.ProjectActivityListResult;
import com.fabbitinc.server.application.project.query.result.ProjectActivityResult;
import com.fabbitinc.server.application.project.query.result.ProjectActivityUserSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectDetailResult;
import com.fabbitinc.server.application.project.query.result.ProjectListResult;
import com.fabbitinc.server.application.project.query.result.ProjectSummaryResult;
import com.fabbitinc.server.application.project.usecase.ArchiveProjectUseCase;
import com.fabbitinc.server.application.project.usecase.CreateProjectUseCase;
import com.fabbitinc.server.application.project.usecase.DeleteProjectUseCase;
import com.fabbitinc.server.application.project.usecase.UnarchiveProjectUseCase;
import com.fabbitinc.server.application.project.usecase.UpdateProjectUseCase;
import com.fabbitinc.server.application.project.usecase.command.ArchiveProjectCommand;
import com.fabbitinc.server.application.project.usecase.command.CreateProjectCommand;
import com.fabbitinc.server.application.project.usecase.command.DeleteProjectCommand;
import com.fabbitinc.server.application.project.usecase.command.UnarchiveProjectCommand;
import com.fabbitinc.server.application.project.usecase.command.UpdateProjectCommand;
import com.fabbitinc.server.application.project.usecase.result.CreateProjectResult;
import com.fabbitinc.server.application.project.usecase.result.UpdateProjectResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
@RequestMapping("/api/v1/projects")
@Tag(name = "projects", description = "프로젝트 관리 API")
public class ProjectController {

    private final ProjectQuery projectQuery;
    private final CreateProjectUseCase createProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final ArchiveProjectUseCase archiveProjectUseCase;
    private final UnarchiveProjectUseCase unarchiveProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;

    @Operation(summary = "POST /api/v1/projects", description = "프로젝트를 생성하고 상세 정보를 반환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse createProject(
            @Parameter(description = "프로젝트 생성 요청")
            @Valid @RequestBody CreateProjectRequest request
    ) {
        CreateProjectResult createResult = createProjectUseCase.execute(
                new CreateProjectCommand(request.name(), request.description())
        );
        ProjectDetailResult detailResult = projectQuery.get(new ProjectDetailCondition(createResult.projectId()));
        return toProjectDetailResponse(detailResult);
    }

    @Operation(summary = "GET /api/v1/projects", description = "프로젝트 목록을 검색/페이징 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public ProjectListResponse listProjects(
            @Parameter(description = "프로젝트 이름 검색어", example = "신규 BOM")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "페이지 시작 오프셋", example = "0")
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @Parameter(description = "조회 건수", example = "20")
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        ProjectListResult result = projectQuery.list(new ProjectListCondition(search, offset, limit));
        return toProjectListResponse(result);
    }

    @Operation(summary = "PATCH /api/v1/projects/{projectId}", description = "프로젝트 이름/설명을 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PatchMapping("/{projectId}")
    public ProjectDetailResponse updateProject(
            @Parameter(description = "수정할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "프로젝트 수정 요청")
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        UpdateProjectResult updateResult = updateProjectUseCase.execute(
                new UpdateProjectCommand(projectId, request.name(), request.description())
        );
        ProjectDetailResult detailResult = projectQuery.get(new ProjectDetailCondition(updateResult.projectId()));
        return toProjectDetailResponse(detailResult);
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}", description = "프로젝트 상세를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{projectId}")
    public ProjectDetailResponse getProject(
            @Parameter(description = "조회할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        return toProjectDetailResponse(projectQuery.get(new ProjectDetailCondition(projectId)));
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/archive", description = "프로젝트를 보관 상태로 전환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "보관 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/{projectId}/archive")
    public ResponseEntity<Void> archiveProject(
            @Parameter(description = "보관할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        archiveProjectUseCase.execute(new ArchiveProjectCommand(projectId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/unarchive", description = "프로젝트 보관을 해제합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "복원 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/{projectId}/unarchive")
    public ResponseEntity<Void> unarchiveProject(
            @Parameter(description = "보관 해제할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        unarchiveProjectUseCase.execute(new UnarchiveProjectCommand(projectId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "DELETE /api/v1/projects/{projectId}", description = "프로젝트를 소프트 삭제합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "삭제할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        deleteProjectUseCase.execute(new DeleteProjectCommand(projectId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}/activities", description = "프로젝트 활동 피드를 cursor 기반으로 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{projectId}/activities")
    public ActivityListResponse getProjectActivities(
            @Parameter(description = "활동 피드를 조회할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "다음 페이지 기준 cursor activity ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @Parameter(description = "조회 건수", example = "20")
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit,
            @Parameter(description = "활동 범위 필터", example = "ALL")
            @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "특정 사용자 활동만 필터링할 사용자 ID")
            @RequestParam(value = "user_id", required = false) UUID userId
    ) {
        ProjectActivityListResult result = projectQuery.listActivities(
                new ProjectActivitiesCondition(projectId, cursor, limit, scope, userId)
        );
        return toActivityListResponse(result);
    }

    private ProjectListResponse toProjectListResponse(ProjectListResult result) {
        return new ProjectListResponse(
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream().map(this::toProjectSummaryResponse).toList()
        );
    }

    private ProjectSummaryResponse toProjectSummaryResponse(ProjectSummaryResult result) {
        return new ProjectSummaryResponse(
                result.id(),
                result.name(),
                result.description(),
                result.partCount(),
                result.isArchived()
        );
    }

    private ProjectDetailResponse toProjectDetailResponse(ProjectDetailResult result) {
        return new ProjectDetailResponse(
                result.id(),
                result.name(),
                result.description(),
                result.partCount(),
                result.isArchived(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private ActivityListResponse toActivityListResponse(ProjectActivityListResult result) {
        Map<String, UserSummaryResponse> users = result.users().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toUserSummaryResponse(entry.getValue())));

        return new ActivityListResponse(
                result.items().stream().map(this::toActivityResponse).toList(),
                result.nextCursor(),
                users
        );
    }

    private ActivityResponse toActivityResponse(ProjectActivityResult result) {
        return new ActivityResponse(
                result.id(),
                result.action(),
                result.scope(),
                result.actorId(),
                result.detail(),
                result.createdAt()
        );
    }

    private UserSummaryResponse toUserSummaryResponse(ProjectActivityUserSummaryResult result) {
        return new UserSummaryResponse(
                result.id(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }
}
