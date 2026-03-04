package com.fabbitinc.server.presentation.change.controller;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.issue.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateChangeRequestRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateCommentRequest;
import com.fabbitinc.server.application.issue.dto.request.SubmitReviewRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncAssigneesRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncIssuesRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncLabelsRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncPartsRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncReviewersRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncTeamAssigneesRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncTeamReviewersRequest;
import com.fabbitinc.server.application.issue.dto.request.UpdateCommentRequest;
import com.fabbitinc.server.application.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestListResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestLookupResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestResponse;
import com.fabbitinc.server.application.issue.dto.response.CommentResponse;
import com.fabbitinc.server.application.issue.dto.response.SubmitReviewResponse;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineResponse;
import com.fabbitinc.server.application.issue.query.IssueQuery;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.AddIssueFilesUseCase;
import com.fabbitinc.server.application.issue.usecase.CloseChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteIssueFileUseCase;
import com.fabbitinc.server.application.issue.usecase.MergeChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.ReopenChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.SubmitChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.SubmitReviewUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncIssuesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncLabelsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncPartsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncReviewersUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamReviewersUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateCommentUseCase;
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
@RequestMapping("/api/v1/changes")
@Tag(name = "changes", description = "변경요청 조회/생성/상태전이 API")
public class ChangeRequestController {

    private final IssueQuery issueQuery;
    private final CreateChangeRequestUseCase createChangeRequestUseCase;
    private final UpdateChangeRequestUseCase updateChangeRequestUseCase;
    private final SubmitChangeRequestUseCase submitChangeRequestUseCase;
    private final MergeChangeRequestUseCase mergeChangeRequestUseCase;
    private final CloseChangeRequestUseCase closeChangeRequestUseCase;
    private final ReopenChangeRequestUseCase reopenChangeRequestUseCase;
    private final SyncIssuesUseCase syncIssuesUseCase;
    private final SyncAssigneesUseCase syncAssigneesUseCase;
    private final SyncTeamAssigneesUseCase syncTeamAssigneesUseCase;
    private final SyncReviewersUseCase syncReviewersUseCase;
    private final SyncTeamReviewersUseCase syncTeamReviewersUseCase;
    private final SubmitReviewUseCase submitReviewUseCase;
    private final SyncLabelsUseCase syncLabelsUseCase;
    private final SyncPartsUseCase syncPartsUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final AddIssueFilesUseCase addIssueFilesUseCase;
    private final DeleteIssueFileUseCase deleteIssueFileUseCase;

    @Operation(
            summary = "GET /api/v1/changes",
            description = "변경요청 목록을 조회합니다"
    )
    @GetMapping
    public ChangeRequestListResponse listChangeRequests(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "cr_state", required = false) String crState,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return issueQuery.listChangeRequests(search, state, crState, offset, limit);
    }

    @Operation(
            summary = "GET /api/v1/changes/lookup",
            description = "변경요청 연결 picker UI용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public ChangeRequestLookupResponse lookupChangeRequests(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return issueQuery.lookupChangeRequests(search, limit);
    }

    @Operation(
            summary = "GET /api/v1/changes/{issueNumber}",
            description = "변경요청 번호로 상세 정보를 조회합니다"
    )
    @GetMapping("/{issueNumber}")
    public ChangeRequestResponse getChangeRequest(
            @PathVariable int issueNumber
    ) {
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/changes",
            description = "변경요청을 생성하고 연관 정보(이슈/담당자/검토자/파일)를 일괄 연결합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestResponse createChangeRequest(
            @Valid @RequestBody CreateChangeRequestRequest request
    ) {
        int issueNumber = createChangeRequestUseCase.execute(request);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "PATCH /api/v1/changes/{issueNumber}",
            description = "변경요청 제목/본문을 수정합니다"
    )
    @PatchMapping("/{issueNumber}")
    public ChangeRequestResponse updateChangeRequest(
            @PathVariable int issueNumber,
            @Valid @RequestBody UpdateIssueRequest request
    ) {
        updateChangeRequestUseCase.execute(issueNumber, request);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/submit",
            description = "변경요청을 제출합니다 (DRAFT -> SUBMITTED)"
    )
    @PostMapping("/{issueNumber}/submit")
    public ChangeRequestResponse submit(
            @PathVariable int issueNumber
    ) {
        submitChangeRequestUseCase.execute(issueNumber);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/merge",
            description = "변경요청을 반영합니다 (SUBMITTED -> MERGED)"
    )
    @PostMapping("/{issueNumber}/merge")
    public ChangeRequestResponse merge(
            @PathVariable int issueNumber
    ) {
        mergeChangeRequestUseCase.execute(issueNumber);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/close",
            description = "변경요청을 닫습니다"
    )
    @PostMapping("/{issueNumber}/close")
    public ChangeRequestResponse close(
            @PathVariable int issueNumber
    ) {
        closeChangeRequestUseCase.execute(issueNumber);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/reopen",
            description = "변경요청을 다시 엽니다 (CLOSED -> SUBMITTED)"
    )
    @PostMapping("/{issueNumber}/reopen")
    public ChangeRequestResponse reopen(
            @PathVariable int issueNumber
    ) {
        reopenChangeRequestUseCase.execute(issueNumber);
        return issueQuery.getChangeRequest(issueNumber);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/issues",
            description = "변경요청에 연결된 이슈 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/issues")
    public SyncDiffResponse syncIssues(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncIssuesRequest request
    ) {
        return syncIssuesUseCase.execute(issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/assignees",
            description = "개인 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/assignees")
    public SyncDiffResponse syncAssignees(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncAssigneesRequest request
    ) {
        return syncAssigneesUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/assigned-teams",
            description = "팀 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/assigned-teams")
    public SyncDiffResponse syncTeamAssignees(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncTeamAssigneesRequest request
    ) {
        return syncTeamAssigneesUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/reviewers",
            description = "개인 검토자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/reviewers")
    public SyncDiffResponse syncReviewers(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncReviewersRequest request
    ) {
        return syncReviewersUseCase.execute(issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/reviewer-teams",
            description = "팀 검토자 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/reviewer-teams")
    public SyncDiffResponse syncTeamReviewers(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncTeamReviewersRequest request
    ) {
        return syncTeamReviewersUseCase.execute(issueNumber, request);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/review",
            description = "검토자가 리뷰 상태(APPROVED|REJECTED)를 제출합니다"
    )
    @PostMapping("/{issueNumber}/review")
    public SubmitReviewResponse submitReview(
            @PathVariable int issueNumber,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        return submitReviewUseCase.execute(issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/labels",
            description = "라벨 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/labels")
    public SyncDiffResponse syncLabels(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncLabelsRequest request
    ) {
        return syncLabelsUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/parts",
            description = "부품 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/parts")
    public SyncDiffResponse syncParts(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncPartsRequest request
    ) {
        return syncPartsUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "GET /api/v1/changes/{issueNumber}/timeline",
            description = "댓글과 활동 이력을 시간순으로 병합 조회합니다"
    )
    @GetMapping("/{issueNumber}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable int issueNumber
    ) {
        return issueQuery.getIssueTimeline(issueNumber, IssueTargetType.CHANGE_REQUEST);
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/comments",
            description = "댓글을 생성합니다"
    )
    @PostMapping("/{issueNumber}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable int issueNumber,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return createCommentUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "PATCH /api/v1/changes/{issueNumber}/comments/{commentId}",
            description = "댓글을 수정합니다"
    )
    @PatchMapping("/{issueNumber}/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable int issueNumber,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return updateCommentUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, commentId, request);
    }

    @Operation(
            summary = "DELETE /api/v1/changes/{issueNumber}/comments/{commentId}",
            description = "댓글을 삭제합니다"
    )
    @DeleteMapping("/{issueNumber}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable int issueNumber,
            @PathVariable UUID commentId
    ) {
        deleteCommentUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/files",
            description = "첨부파일을 배치 연결합니다"
    )
    @PostMapping("/{issueNumber}/files")
    public List<FileItemResponse> addFiles(
            @PathVariable int issueNumber,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        return addIssueFilesUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, request);
    }

    @Operation(
            summary = "DELETE /api/v1/changes/{issueNumber}/files/{fileId}",
            description = "첨부파일 1건을 삭제(soft delete)합니다"
    )
    @DeleteMapping("/{issueNumber}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable int issueNumber,
            @PathVariable UUID fileId
    ) {
        deleteIssueFileUseCase.execute(IssueTargetType.CHANGE_REQUEST, issueNumber, fileId);
        return ResponseEntity.noContent().build();
    }
}
