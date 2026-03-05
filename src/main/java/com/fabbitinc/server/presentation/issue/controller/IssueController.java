package com.fabbitinc.server.presentation.issue.controller;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.issue.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateCommentRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateIssueRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncAssigneesRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncChangesRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncLabelsRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncPartsRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncTeamAssigneesRequest;
import com.fabbitinc.server.application.issue.dto.request.UpdateCommentRequest;
import com.fabbitinc.server.application.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.application.issue.dto.response.CommentResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueListResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueLookupResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueResponse;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineResponse;
import com.fabbitinc.server.application.issue.query.IssueQuery;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.AddIssueFilesUseCase;
import com.fabbitinc.server.application.issue.usecase.CloseIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteIssueFileUseCase;
import com.fabbitinc.server.application.issue.usecase.ReopenIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncChangesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncLabelsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncPartsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateIssueUseCase;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/issues")
@Tag(name = "issues", description = "이슈 조회/생성/수정/연결 API")
public class IssueController {

    private final IssueQuery issueQuery;
    private final CreateIssueUseCase createIssueUseCase;
    private final UpdateIssueUseCase updateIssueUseCase;
    private final CloseIssueUseCase closeIssueUseCase;
    private final ReopenIssueUseCase reopenIssueUseCase;
    private final SyncAssigneesUseCase syncAssigneesUseCase;
    private final SyncTeamAssigneesUseCase syncTeamAssigneesUseCase;
    private final SyncChangesUseCase syncChangesUseCase;
    private final SyncLabelsUseCase syncLabelsUseCase;
    private final SyncPartsUseCase syncPartsUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final AddIssueFilesUseCase addIssueFilesUseCase;
    private final DeleteIssueFileUseCase deleteIssueFileUseCase;

    @Operation(
            summary = "GET /api/v1/issues/lookup",
            description = "이슈 연결 picker UI용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public IssueLookupResponse lookupIssues(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) IssueType type,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return issueQuery.lookupIssues(search, type, limit);
    }

    @Operation(
            summary = "GET /api/v1/issues",
            description = "일반 이슈(CHANGE_REQUEST 제외) 목록을 조회합니다"
    )
    @GetMapping
    public IssueListResponse listIssues(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "state", required = false) IssueState state,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return issueQuery.listIssues(search, state, offset, limit);
    }

    @Operation(
            summary = "GET /api/v1/issues/{issueNumber}",
            description = "이슈 번호로 상세 정보를 조회합니다"
    )
    @GetMapping("/{issueNumber}")
    public IssueResponse getIssue(
            @PathVariable int issueNumber
    ) {
        return issueQuery.getIssue(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/issues",
            description = "이슈를 생성하고 연관 정보(부품/담당자/라벨/파일)를 일괄 연결합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(
            @Valid @RequestBody CreateIssueRequest request
    ) {
        int issueNumber = createIssueUseCase.execute(request);
        return issueQuery.getIssue(issueNumber);
    }

    @Operation(
            summary = "PATCH /api/v1/issues/{issueNumber}",
            description = "이슈 제목/본문을 수정합니다"
    )
    @PatchMapping("/{issueNumber}")
    public IssueResponse updateIssue(
            @PathVariable int issueNumber,
            @Valid @RequestBody UpdateIssueRequest request
    ) {
        updateIssueUseCase.execute(issueNumber, request);
        return issueQuery.getIssue(issueNumber);
    }

    @Operation(
            summary = "PUT /api/v1/issues/{issueNumber}/assignees",
            description = "개인 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/assignees")
    public SyncDiffResponse syncAssignees(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncAssigneesRequest request
    ) {
        return syncAssigneesUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/issues/{issueNumber}/assigned-teams",
            description = "팀 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/assigned-teams")
    public SyncDiffResponse syncTeamAssignees(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncTeamAssigneesRequest request
    ) {
        return syncTeamAssigneesUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/issues/{issueNumber}/changes",
            description = "이슈에 연결된 변경요청 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/changes")
    public SyncDiffResponse syncChanges(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncChangesRequest request
    ) {
        return syncChangesUseCase.execute(issueNumber, request);
    }

    @Operation(
            summary = "POST /api/v1/issues/{issueNumber}/close",
            description = "이슈를 닫습니다 (OPEN -> CLOSED)"
    )
    @PostMapping("/{issueNumber}/close")
    public IssueResponse closeIssue(
            @PathVariable int issueNumber
    ) {
        closeIssueUseCase.execute(issueNumber);
        return issueQuery.getIssue(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/issues/{issueNumber}/reopen",
            description = "이슈를 다시 엽니다 (CLOSED -> OPEN)"
    )
    @PostMapping("/{issueNumber}/reopen")
    public IssueResponse reopenIssue(
            @PathVariable int issueNumber
    ) {
        reopenIssueUseCase.execute(issueNumber);
        return issueQuery.getIssue(issueNumber);
    }

    @Operation(
            summary = "PUT /api/v1/issues/{issueNumber}/labels",
            description = "라벨 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/labels")
    public SyncDiffResponse syncLabels(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncLabelsRequest request
    ) {
        return syncLabelsUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/issues/{issueNumber}/parts",
            description = "부품 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/parts")
    public SyncDiffResponse syncParts(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncPartsRequest request
    ) {
        return syncPartsUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "GET /api/v1/issues/{issueNumber}/timeline",
            description = "댓글과 활동 이력을 시간순으로 병합 조회합니다"
    )
    @GetMapping("/{issueNumber}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable int issueNumber
    ) {
        return issueQuery.getIssueTimeline(issueNumber, IssueTargetType.ISSUE);
    }

    @Operation(
            summary = "POST /api/v1/issues/{issueNumber}/comments",
            description = "댓글을 생성합니다"
    )
    @PostMapping("/{issueNumber}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable int issueNumber,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return createCommentUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "PATCH /api/v1/issues/{issueNumber}/comments/{commentId}",
            description = "댓글을 수정합니다"
    )
    @PatchMapping("/{issueNumber}/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable int issueNumber,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return updateCommentUseCase.execute(IssueTargetType.ISSUE, issueNumber, commentId, request);
    }

    @Operation(
            summary = "DELETE /api/v1/issues/{issueNumber}/comments/{commentId}",
            description = "댓글을 삭제합니다"
    )
    @DeleteMapping("/{issueNumber}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable int issueNumber,
            @PathVariable UUID commentId
    ) {
        deleteCommentUseCase.execute(IssueTargetType.ISSUE, issueNumber, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "POST /api/v1/issues/{issueNumber}/files",
            description = "첨부파일을 배치 연결합니다"
    )
    @PostMapping("/{issueNumber}/files")
    public List<FileItemResponse> addFiles(
            @PathVariable int issueNumber,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        return addIssueFilesUseCase.execute(IssueTargetType.ISSUE, issueNumber, request);
    }

    @Operation(
            summary = "DELETE /api/v1/issues/{issueNumber}/files/{fileId}",
            description = "첨부파일 1건을 삭제(soft delete)합니다"
    )
    @DeleteMapping("/{issueNumber}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable int issueNumber,
            @PathVariable UUID fileId
    ) {
        deleteIssueFileUseCase.execute(IssueTargetType.ISSUE, issueNumber, fileId);
        return ResponseEntity.noContent().build();
    }
}
