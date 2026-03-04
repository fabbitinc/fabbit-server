package com.fabbitinc.server.presentation.project.controller;

import com.fabbitinc.server.application.project.dto.request.AddMembersRequest;
import com.fabbitinc.server.application.project.dto.request.ManageMembersRequest;
import com.fabbitinc.server.application.project.dto.response.ManageMembersResponse;
import com.fabbitinc.server.application.project.dto.response.MemberLookupResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectMemberListResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.usecase.AddProjectMembersUseCase;
import com.fabbitinc.server.application.project.usecase.RemoveProjectMembersUseCase;
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
@RequestMapping("/api/v1/projects/{projectId}/members")
@Tag(name = "project-members", description = "프로젝트 멤버 관리 API")
public class ProjectMemberController {

    private final ProjectQuery projectQuery;
    private final AddProjectMembersUseCase addProjectMembersUseCase;
    private final RemoveProjectMembersUseCase removeProjectMembersUseCase;

    @Operation(summary = "GET /api/v1/projects/{projectId}/members/lookup", description = "프로젝트 멤버 picker용 lookup 목록을 조회합니다")
    @GetMapping("/lookup")
    public MemberLookupResponse lookupMembers(
            @PathVariable UUID projectId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return projectQuery.lookupMembers(projectId, search, limit);
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}/members", description = "프로젝트 멤버 목록을 조회합니다")
    @GetMapping
    public ProjectMemberListResponse listProjectMembers(
            @PathVariable UUID projectId
    ) {
        return projectQuery.listMembers(projectId);
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/members", description = "프로젝트에 멤버를 배치 추가합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManageMembersResponse addProjectMembers(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddMembersRequest request
    ) {
        return addProjectMembersUseCase.execute(projectId,
                request.userIds(),
                request.role()
        );
    }

    @Operation(summary = "DELETE /api/v1/projects/{projectId}/members", description = "프로젝트에서 멤버를 배치 제거합니다")
    @DeleteMapping
    public ResponseEntity<Void> removeProjectMembers(
            @PathVariable UUID projectId,
            @Valid @RequestBody ManageMembersRequest request
    ) {
        removeProjectMembersUseCase.execute(projectId, request.userIds());
        return ResponseEntity.noContent().build();
    }
}
