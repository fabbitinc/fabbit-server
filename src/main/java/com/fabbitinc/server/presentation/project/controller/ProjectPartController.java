package com.fabbitinc.server.presentation.project.controller;

import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.project.dto.request.LinkPartsRequest;
import com.fabbitinc.server.application.project.dto.response.LinkPartsResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectPartsResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.usecase.LinkProjectPartsUseCase;
import com.fabbitinc.server.application.project.usecase.UnlinkProjectPartsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/parts")
@Tag(name = "project-parts", description = "프로젝트 부품 연결 API")
public class ProjectPartController {

    private final ProjectQuery projectQuery;
    private final LinkProjectPartsUseCase linkProjectPartsUseCase;
    private final UnlinkProjectPartsUseCase unlinkProjectPartsUseCase;

    @Operation(summary = "GET /api/v1/projects/{projectId}/parts/lookup", description = "부품 picker용 lookup 목록을 조회합니다")
    @GetMapping("/lookup")
    public PartLookupResponse lookupParts(
            @PathVariable UUID projectId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "exclude_linked", defaultValue = "false") boolean excludeLinked,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return projectQuery.lookupParts(projectId, search, excludeLinked, limit);
    }

    @Operation(summary = "POST /api/v1/projects/{projectId}/parts", description = "프로젝트에 부품을 배치 연결합니다")
    @PostMapping
    public LinkPartsResponse linkParts(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkPartsRequest request
    ) {
        return linkProjectPartsUseCase.execute(projectId, request.partIds());
    }

    @Operation(summary = "DELETE /api/v1/projects/{projectId}/parts", description = "프로젝트에서 부품을 배치 해제합니다")
    @DeleteMapping
    public ResponseEntity<Void> unlinkParts(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkPartsRequest request
    ) {
        unlinkProjectPartsUseCase.execute(projectId, request.partIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "GET /api/v1/projects/{projectId}/parts", description = "프로젝트에 연결된 부품 목록을 조회합니다")
    @GetMapping
    public ProjectPartsResponse getProjectParts(
            @PathVariable UUID projectId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return projectQuery.getProjectParts(projectId, search, offset, limit);
    }
}
