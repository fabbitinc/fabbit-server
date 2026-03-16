package com.fabbitinc.server.presentation.project.controller;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import com.fabbitinc.server.presentation.project.dto.request.AddMembersRequest;
import com.fabbitinc.server.presentation.project.dto.request.ManageMembersRequest;
import com.fabbitinc.server.presentation.project.dto.response.ManageMembersResponse;
import com.fabbitinc.server.presentation.project.dto.response.MemberLookupResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectMemberListResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectMemberSummaryResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectUserSummaryResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.ProjectMembersCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectMembersLookupCondition;
import com.fabbitinc.server.application.project.query.result.MemberLookupResult;
import com.fabbitinc.server.application.project.query.result.ProjectMemberListResult;
import com.fabbitinc.server.application.project.query.result.ProjectMemberSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectUserSummaryResult;
import com.fabbitinc.server.application.project.usecase.AddProjectMembersUseCase;
import com.fabbitinc.server.application.project.usecase.RemoveProjectMembersUseCase;
import com.fabbitinc.server.application.project.usecase.command.AddProjectMembersCommand;
import com.fabbitinc.server.application.project.usecase.command.RemoveProjectMembersCommand;
import com.fabbitinc.server.application.project.usecase.result.AddProjectMembersResult;
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
@RequestMapping("/api/v1/projects/{projectId}/members")
@Tag(name = "project-members", description = "프로젝트 멤버 관리 API")
public class ProjectMemberController {

    private final ProjectQuery projectQuery;
    private final AddProjectMembersUseCase addProjectMembersUseCase;
    private final RemoveProjectMembersUseCase removeProjectMembersUseCase;

    @Operation(summary = "GET /api/v1/projects/{projectId}/members/lookup", description = "프로젝트 멤버 picker용 lookup 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/lookup")
    public MemberLookupResponse lookupMembers(
            @Parameter(description = "멤버 후보를 조회할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "멤버 이름/이메일 검색어", example = "홍길동")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        MemberLookupResult result = projectQuery.lookupMembers(
                new ProjectMembersLookupCondition(projectId, search, limit)
        );
        return new MemberLookupResponse(result.items().stream().map(this::toProjectUserSummaryResponse).toList());
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}/members", description = "프로젝트 멤버 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public ProjectMemberListResponse listProjectMembers(
            @Parameter(description = "멤버 목록을 조회할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        ProjectMemberListResult result = projectQuery.listMembers(new ProjectMembersCondition(projectId));
        return new ProjectMemberListResponse(result.items().stream().map(this::toProjectMemberSummaryResponse).toList());
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/members", description = "프로젝트에 멤버를 배치 추가합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManageMembersResponse addProjectMembers(
            @Parameter(description = "멤버를 추가할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "프로젝트 멤버 추가 요청")
            @Valid @RequestBody AddMembersRequest request
    ) {
        AddProjectMembersResult result = addProjectMembersUseCase.execute(
                new AddProjectMembersCommand(projectId, request.userIds(), request.role())
        );
        return new ManageMembersResponse(result.count());
    }

    @Operation(summary = "DELETE /api/v1/projects/{projectId}/members", description = "프로젝트에서 멤버를 배치 제거합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "제거 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping
    public ResponseEntity<Void> removeProjectMembers(
            @Parameter(description = "멤버를 제거할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "프로젝트 멤버 제거 요청")
            @Valid @RequestBody ManageMembersRequest request
    ) {
        removeProjectMembersUseCase.execute(new RemoveProjectMembersCommand(projectId, request.userIds()));
        return ResponseEntity.noContent().build();
    }

    private ProjectUserSummaryResponse toProjectUserSummaryResponse(ProjectUserSummaryResult result) {
        return new ProjectUserSummaryResponse(
                result.id(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private ProjectMemberSummaryResponse toProjectMemberSummaryResponse(ProjectMemberSummaryResult result) {
        return new ProjectMemberSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl(),
                result.role()
        );
    }
}
