package com.fabbitinc.server.presentation.project.controller;

import com.fabbitinc.server.application.activity.dto.response.ActivityListResponse;
import com.fabbitinc.server.application.project.dto.request.CreateProjectRequest;
import com.fabbitinc.server.application.project.dto.request.UpdateProjectRequest;
import com.fabbitinc.server.application.project.dto.response.ProjectDetailResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectListResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.usecase.ArchiveProjectUseCase;
import com.fabbitinc.server.application.project.usecase.CreateProjectUseCase;
import com.fabbitinc.server.application.project.usecase.DeleteProjectUseCase;
import com.fabbitinc.server.application.project.usecase.UnarchiveProjectUseCase;
import com.fabbitinc.server.application.project.usecase.UpdateProjectUseCase;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse createProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        UUID projectId = createProjectUseCase.execute(authorizationHeader, request.name(), request.description());
        return projectQuery.getProjectDetail(authorizationHeader, projectId);
    }

    @Operation(summary = "GET /api/v1/projects", description = "프로젝트 목록을 검색/페이징 조회합니다")
    @GetMapping
    public ProjectListResponse listProjects(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return projectQuery.listProjects(authorizationHeader, search, offset, limit);
    }

    @Operation(summary = "PATCH /api/v1/projects/{projectId}", description = "프로젝트 이름/설명을 수정합니다")
    @PatchMapping("/{projectId}")
    public ProjectDetailResponse updateProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        UUID updatedProjectId = updateProjectUseCase.execute(
                authorizationHeader,
                projectId,
                request.name(),
                request.description()
        );
        return projectQuery.getProjectDetail(authorizationHeader, updatedProjectId);
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}", description = "프로젝트 상세를 조회합니다")
    @GetMapping("/{projectId}")
    public ProjectDetailResponse getProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId
    ) {
        return projectQuery.getProjectDetail(authorizationHeader, projectId);
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/archive", description = "프로젝트를 보관 상태로 전환합니다")
    @PostMapping("/{projectId}/archive")
    public ResponseEntity<Void> archiveProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId
    ) {
        archiveProjectUseCase.execute(authorizationHeader, projectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/unarchive", description = "프로젝트 보관을 해제합니다")
    @PostMapping("/{projectId}/unarchive")
    public ResponseEntity<Void> unarchiveProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId
    ) {
        unarchiveProjectUseCase.execute(authorizationHeader, projectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "DELETE /api/v1/projects/{projectId}", description = "프로젝트를 소프트 삭제합니다")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId
    ) {
        deleteProjectUseCase.execute(authorizationHeader, projectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}/activities", description = "프로젝트 활동 피드를 cursor 기반으로 조회합니다")
    @GetMapping("/{projectId}/activities")
    public ActivityListResponse getProjectActivities(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID projectId,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "user_id", required = false) UUID userId
    ) {
        return projectQuery.getActivities(authorizationHeader, projectId, cursor, limit, scope, userId);
    }
}
