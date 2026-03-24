package com.fabbitinc.server.presentation.project.controller;

import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.ProjectPartsCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectPartsLookupCondition;
import com.fabbitinc.server.application.project.query.result.ProjectPartLookupItemResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartLookupResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartsResult;
import com.fabbitinc.server.application.project.usecase.LinkProjectPartsUseCase;
import com.fabbitinc.server.application.project.usecase.UnlinkProjectPartsUseCase;
import com.fabbitinc.server.application.project.usecase.command.LinkProjectPartsCommand;
import com.fabbitinc.server.application.project.usecase.command.UnlinkProjectPartsCommand;
import com.fabbitinc.server.application.project.usecase.result.LinkProjectPartsResult;
import com.fabbitinc.server.presentation.project.dto.request.LinkPartsRequest;
import com.fabbitinc.server.presentation.project.dto.response.LinkPartsResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectPartLookupItemResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectPartLookupResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectPartSummaryResponse;
import com.fabbitinc.server.presentation.project.dto.response.ProjectPartsResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/parts")
@Tag(name = "project-parts", description = "프로젝트 부품 연결 API")
public class ProjectPartController {

    private final ProjectQuery projectQuery;
    private final LinkProjectPartsUseCase linkProjectPartsUseCase;
    private final UnlinkProjectPartsUseCase unlinkProjectPartsUseCase;

    @Operation(summary = "부품 picker용 lookup 목록을 조회합니다", description = "부품 picker용 lookup 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/lookup")
    public ProjectPartLookupResponse lookupParts(
            @Parameter(description = "부품 후보를 조회할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "부품명/번호 검색어", example = "BRKT-001")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "이미 연결된 부품 제외 여부", example = "false")
            @RequestParam(value = "exclude_linked", defaultValue = "false") boolean excludeLinked,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        ProjectPartLookupResult result = projectQuery.lookupParts(
                new ProjectPartsLookupCondition(projectId, search, excludeLinked, limit)
        );
        return new ProjectPartLookupResponse(result.items().stream().map(this::toProjectPartLookupItemResponse).toList());
    }

    @Operation(summary = "프로젝트에 부품을 배치 연결합니다", description = "프로젝트에 부품을 배치 연결합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연결 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    public LinkPartsResponse linkParts(
            @Parameter(description = "부품을 연결할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "프로젝트 부품 연결 요청")
            @Valid @RequestBody LinkPartsRequest request
    ) {
        LinkProjectPartsResult result = linkProjectPartsUseCase.execute(
                new LinkProjectPartsCommand(projectId, request.partIds())
        );
        return new LinkPartsResponse(result.linkedCount());
    }

    @Operation(summary = "프로젝트에서 부품을 배치 해제합니다", description = "프로젝트에서 부품을 배치 해제합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "해제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping
    public ResponseEntity<Void> unlinkParts(
            @Parameter(description = "부품 연결을 해제할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "프로젝트 부품 연결 해제 요청")
            @Valid @RequestBody LinkPartsRequest request
    ) {
        unlinkProjectPartsUseCase.execute(new UnlinkProjectPartsCommand(projectId, request.partIds()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "프로젝트에 연결된 부품 목록을 조회합니다", description = "프로젝트에 연결된 부품 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public ProjectPartsResponse getProjectParts(
            @Parameter(description = "부품 목록을 조회할 프로젝트 ID")
            @PathVariable UUID projectId,
            @Parameter(description = "부품명/번호 검색어", example = "BRKT-001")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "페이지 시작 오프셋", example = "0")
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @Parameter(description = "조회 건수", example = "20")
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        ProjectPartsResult result = projectQuery.listParts(new ProjectPartsCondition(projectId, search, offset, limit));
        return new ProjectPartsResponse(
                result.total(),
                result.items().stream().map(this::toProjectPartSummaryResponse).toList()
        );
    }

    private ProjectPartLookupItemResponse toProjectPartLookupItemResponse(ProjectPartLookupItemResult result) {
        return new ProjectPartLookupItemResponse(result.id(), result.partNumber(), result.name());
    }

    private ProjectPartSummaryResponse toProjectPartSummaryResponse(ProjectPartSummaryResult result) {
        return new ProjectPartSummaryResponse(result.id(), result.partNumber(), result.name());
    }
}
