package com.fabbitinc.server.presentation.change.controller;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.issue.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateChangeRequestRequest;
import com.fabbitinc.server.application.issue.dto.request.CreateCommentRequest;
import com.fabbitinc.server.application.issue.dto.request.SubmitReviewRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncPartRevisionsRequest;
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
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestPartRevisionResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestLookupItemResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestLookupResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.CommentResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueUserSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.LabelBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.LinkedIssueBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.PartBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.ReviewerSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.SubmitReviewResponse;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineItemResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineItemType;
import com.fabbitinc.server.application.issue.dto.response.TimelineResponse;
import com.fabbitinc.server.application.issue.query.IssueQuery;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestListCondition;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueTimelineCondition;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestDetailResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestListResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestLookupResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestPartRevisionResult;
import com.fabbitinc.server.application.issue.query.result.IssueFileItemResult;
import com.fabbitinc.server.application.issue.query.result.IssueTimelineResult;
import com.fabbitinc.server.application.issue.query.result.IssueUserSummaryResult;
import com.fabbitinc.server.application.issue.query.result.LabelBadgeResult;
import com.fabbitinc.server.application.issue.query.result.LinkedIssueBadgeResult;
import com.fabbitinc.server.application.issue.query.result.PartBadgeResult;
import com.fabbitinc.server.application.issue.query.result.ReviewerSummaryResult;
import com.fabbitinc.server.application.issue.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.issue.query.result.TimelineItemTypeResult;
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
import com.fabbitinc.server.application.issue.usecase.SyncChangeRequestPartRevisionsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncPartsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncReviewersUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamReviewersUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateChangeRequestUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.result.AttachedFileResult;
import com.fabbitinc.server.application.issue.usecase.result.CommentResult;
import com.fabbitinc.server.application.issue.usecase.result.SubmitReviewResult;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/changes")
@Tag(name = "changes", description = "변경요청 조회/생성/상태전이 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
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
    private final SyncChangeRequestPartRevisionsUseCase syncChangeRequestPartRevisionsUseCase;
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
            @Parameter(description = "변경요청 제목 검색어", example = "품번")
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "cr_state", required = false) String crState,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toChangeRequestListResponse(
                issueQuery.listChangeRequests(new ChangeRequestListCondition(search, state, crState, offset, limit))
        );
    }

    @Operation(
            summary = "GET /api/v1/changes/lookup",
            description = "변경요청 연결 picker UI용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public ChangeRequestLookupResponse lookupChangeRequests(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toChangeRequestLookupResponse(issueQuery.lookupChangeRequests(new ChangeRequestLookupCondition(search, limit)));
    }

    @Operation(
            summary = "GET /api/v1/changes/{issueNumber}",
            description = "변경요청 번호로 상세 정보를 조회합니다"
    )
    @GetMapping("/{issueNumber}")
    public ChangeRequestResponse getChangeRequest(
            @Parameter(description = "조회할 변경요청 번호", example = "201")
            @PathVariable int issueNumber
    ) {
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
    }

    @Operation(
            summary = "POST /api/v1/changes",
            description = "변경요청을 생성하고 연관 정보(이슈/담당자/검토자/파일)를 일괄 연결합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestResponse createChangeRequest(
            @Parameter(description = "변경요청 생성 요청")
            @Valid @RequestBody CreateChangeRequestRequest request
    ) {
        CreateChangeRequestUseCase.CreateChangeRequestResult result = createChangeRequestUseCase.execute(
                new CreateChangeRequestUseCase.CreateChangeRequestCommand(
                        request.title(),
                        request.body(),
                        request.issueNumber(),
                        request.partIds(),
                        request.partRevisions().stream()
                                .map(item -> new CreateChangeRequestUseCase.CreateChangeRequestCommand.PartRevisionTarget(
                                        item.partNumber(),
                                        item.baseRevisionCode(),
                                        item.draftKey()
                                ))
                                .toList(),
                        request.assigneeUserIds(),
                        request.teamAssigneeIds(),
                        request.labelIds(),
                        request.fileIds(),
                        request.reviewerUserIds(),
                        request.teamReviewerIds()
                )
        );
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(result.issueNumber())));
    }

    @Operation(
            summary = "PATCH /api/v1/changes/{issueNumber}",
            description = "변경요청 제목/본문을 수정합니다"
    )
    @PatchMapping("/{issueNumber}")
    public ChangeRequestResponse updateChangeRequest(
            @Parameter(description = "수정할 변경요청 번호", example = "201")
            @PathVariable int issueNumber,
            @Parameter(description = "변경요청 수정 요청")
            @Valid @RequestBody UpdateIssueRequest request
    ) {
        updateChangeRequestUseCase.execute(
                new UpdateChangeRequestUseCase.UpdateChangeRequestCommand(issueNumber, request.title(), request.body())
        );
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/submit",
            description = "변경요청을 제출합니다 (DRAFT -> SUBMITTED)"
    )
    @PostMapping("/{issueNumber}/submit")
    public ChangeRequestResponse submit(
            @PathVariable int issueNumber
    ) {
        submitChangeRequestUseCase.execute(new SubmitChangeRequestUseCase.SubmitChangeRequestCommand(issueNumber));
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/merge",
            description = "변경요청을 반영합니다 (SUBMITTED -> MERGED)"
    )
    @PostMapping("/{issueNumber}/merge")
    public ChangeRequestResponse merge(
            @PathVariable int issueNumber
    ) {
        mergeChangeRequestUseCase.execute(new MergeChangeRequestUseCase.MergeChangeRequestCommand(issueNumber));
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/close",
            description = "변경요청을 닫습니다"
    )
    @PostMapping("/{issueNumber}/close")
    public ChangeRequestResponse close(
            @PathVariable int issueNumber
    ) {
        closeChangeRequestUseCase.execute(new CloseChangeRequestUseCase.CloseChangeRequestCommand(issueNumber));
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
    }

    @Operation(
            summary = "POST /api/v1/changes/{issueNumber}/reopen",
            description = "변경요청을 다시 엽니다 (CLOSED -> SUBMITTED)"
    )
    @PostMapping("/{issueNumber}/reopen")
    public ChangeRequestResponse reopen(
            @PathVariable int issueNumber
    ) {
        reopenChangeRequestUseCase.execute(new ReopenChangeRequestUseCase.ReopenChangeRequestCommand(issueNumber));
        return toChangeRequestResponse(issueQuery.getChangeRequest(new ChangeRequestDetailCondition(issueNumber)));
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
        return toSyncDiffResponse(
                syncIssuesUseCase.execute(new SyncIssuesUseCase.SyncIssuesCommand(issueNumber, request.issueIds()))
        );
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
        return toSyncDiffResponse(
                syncAssigneesUseCase.execute(
                        new SyncAssigneesUseCase.SyncAssigneesCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.userIds()
                        )
                )
        );
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
        return toSyncDiffResponse(
                syncTeamAssigneesUseCase.execute(
                        new SyncTeamAssigneesUseCase.SyncTeamAssigneesCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.teamIds()
                        )
                )
        );
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
        return toSyncDiffResponse(
                syncReviewersUseCase.execute(new SyncReviewersUseCase.SyncReviewersCommand(issueNumber, request.userIds()))
        );
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
        return toSyncDiffResponse(
                syncTeamReviewersUseCase.execute(
                        new SyncTeamReviewersUseCase.SyncTeamReviewersCommand(issueNumber, request.teamIds())
                )
        );
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
        return toSubmitReviewResponse(
                submitReviewUseCase.execute(new SubmitReviewUseCase.SubmitReviewCommand(issueNumber, request.status()))
        );
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
        return toSyncDiffResponse(
                syncLabelsUseCase.execute(
                        new SyncLabelsUseCase.SyncLabelsCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.labelIds()
                        )
                )
        );
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
        return toSyncDiffResponse(
                syncPartsUseCase.execute(
                        new SyncPartsUseCase.SyncPartsCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.partIds()
                        )
                )
        );
    }

    @Operation(
            summary = "PUT /api/v1/changes/{issueNumber}/part-revisions",
            description = "변경요청에 연결할 부품 초안 목록을 동기화합니다"
    )
    @PutMapping("/{issueNumber}/part-revisions")
    public SyncDiffResponse syncPartRevisions(
            @PathVariable int issueNumber,
            @Valid @RequestBody SyncPartRevisionsRequest request
    ) {
        return toSyncDiffResponse(
                syncChangeRequestPartRevisionsUseCase.execute(
                        new SyncChangeRequestPartRevisionsUseCase.SyncChangeRequestPartRevisionsCommand(
                                issueNumber,
                                request.items().stream()
                                        .map(item -> new SyncChangeRequestPartRevisionsUseCase.SyncChangeRequestPartRevisionsCommand.Item(
                                                item.partNumber(),
                                                item.baseRevisionCode(),
                                                item.draftKey()
                                        ))
                                        .toList()
                        )
                )
        );
    }

    @Operation(
            summary = "GET /api/v1/changes/{issueNumber}/timeline",
            description = "댓글과 활동 이력을 시간순으로 병합 조회합니다"
    )
    @GetMapping("/{issueNumber}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable int issueNumber
    ) {
        return toTimelineResponse(
                issueQuery.getTimeline(new IssueTimelineCondition(issueNumber, IssueTargetType.CHANGE_REQUEST))
        );
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
        return toCommentResponse(
                createCommentUseCase.execute(
                        new CreateCommentUseCase.CreateCommentCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.body()
                        )
                )
        );
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
        return toCommentResponse(
                updateCommentUseCase.execute(
                        new UpdateCommentUseCase.UpdateCommentCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                commentId,
                                request.body()
                        )
                )
        );
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
        deleteCommentUseCase.execute(
                new DeleteCommentUseCase.DeleteCommentCommand(IssueTargetType.CHANGE_REQUEST, issueNumber, commentId)
        );
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
        return addIssueFilesUseCase.execute(
                        new AddIssueFilesUseCase.AddIssueFilesCommand(
                                IssueTargetType.CHANGE_REQUEST,
                                issueNumber,
                                request.fileIds()
                        )
                )
                .stream()
                .map(this::toFileItemResponse)
                .toList();
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
        deleteIssueFileUseCase.execute(
                new DeleteIssueFileUseCase.DeleteIssueFileCommand(IssueTargetType.CHANGE_REQUEST, issueNumber, fileId)
        );
        return ResponseEntity.noContent().build();
    }

    private SyncDiffResponse toSyncDiffResponse(SyncDiffResult result) {
        return new SyncDiffResponse(result.addedCount(), result.removedCount());
    }

    private SubmitReviewResponse toSubmitReviewResponse(SubmitReviewResult result) {
        return new SubmitReviewResponse(result.reviewStatus(), result.reviewedAt());
    }

    private CommentResponse toCommentResponse(CommentResult result) {
        return new CommentResponse(
                result.id(),
                result.issueId(),
                result.body(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                result.createdBy()
        );
    }

    private ChangeRequestListResponse toChangeRequestListResponse(ChangeRequestListResult result) {
        return new ChangeRequestListResponse(
                result.openCount(),
                result.closedCount(),
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream().map(this::toChangeRequestSummaryResponse).toList()
        );
    }

    private ChangeRequestSummaryResponse toChangeRequestSummaryResponse(ChangeRequestListResult.Item result) {
        return new ChangeRequestSummaryResponse(
                result.id(),
                result.number(),
                result.type(),
                result.title(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                toIssueUserSummaryResponse(result.createdBy()),
                result.labels().stream().map(this::toLabelBadgeResponse).toList(),
                result.assignees().stream().map(this::toIssueUserSummaryResponse).toList(),
                result.assignedTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.reviewers().stream().map(this::toReviewerSummaryResponse).toList(),
                result.reviewerTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.parts().stream().map(this::toPartBadgeResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.crState(),
                result.mergedAt(),
                result.mergedBy()
        );
    }

    private ChangeRequestLookupResponse toChangeRequestLookupResponse(ChangeRequestLookupResult result) {
        return new ChangeRequestLookupResponse(
                result.items().stream()
                        .map(item -> new ChangeRequestLookupItemResponse(
                                item.id(),
                                item.number(),
                                item.title(),
                                item.state(),
                                item.crState()
                        ))
                        .toList()
        );
    }

    private ChangeRequestResponse toChangeRequestResponse(ChangeRequestDetailResult result) {
        return new ChangeRequestResponse(
                result.id(),
                result.number(),
                result.type(),
                result.title(),
                result.body(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                toIssueUserSummaryResponse(result.createdBy()),
                result.labels().stream().map(this::toLabelBadgeResponse).toList(),
                result.assignees().stream().map(this::toIssueUserSummaryResponse).toList(),
                result.assignedTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.reviewers().stream().map(this::toReviewerSummaryResponse).toList(),
                result.reviewerTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.parts().stream().map(this::toPartBadgeResponse).toList(),
                result.partRevisions().stream().map(this::toPartRevisionResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.crState(),
                result.mergedAt(),
                result.mergedBy(),
                result.linkedIssues().stream().map(this::toLinkedIssueBadgeResponse).toList()
        );
    }

    private TimelineResponse toTimelineResponse(IssueTimelineResult result) {
        Map<String, IssueUserSummaryResponse> users = new LinkedHashMap<>();
        result.users().forEach((userId, user) -> users.put(userId, toIssueUserSummaryResponse(user)));

        return new TimelineResponse(
                result.items().stream().map(this::toTimelineItemResponse).toList(),
                users
        );
    }

    private TimelineItemResponse toTimelineItemResponse(IssueTimelineResult.Item result) {
        return new TimelineItemResponse(
                toTimelineItemType(result.type()),
                result.id(),
                result.action(),
                result.scope(),
                result.actorId(),
                result.detail(),
                result.body(),
                result.authorId(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified()
        );
    }

    private TimelineItemType toTimelineItemType(TimelineItemTypeResult result) {
        if (result == TimelineItemTypeResult.ACTIVITY) {
            return TimelineItemType.ACTIVITY;
        }
        return TimelineItemType.COMMENT;
    }

    private IssueUserSummaryResponse toIssueUserSummaryResponse(IssueUserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new IssueUserSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private LabelBadgeResponse toLabelBadgeResponse(LabelBadgeResult result) {
        return new LabelBadgeResponse(result.id(), result.name(), result.color());
    }

    private TeamBadgeResponse toTeamBadgeResponse(TeamBadgeResult result) {
        return new TeamBadgeResponse(result.id(), result.name());
    }

    private PartBadgeResponse toPartBadgeResponse(PartBadgeResult result) {
        return new PartBadgeResponse(result.id(), result.partNumber(), result.name());
    }

    private ChangeRequestPartRevisionResponse toPartRevisionResponse(ChangeRequestPartRevisionResult result) {
        return new ChangeRequestPartRevisionResponse(
                result.revisionId(),
                result.partId(),
                result.partNumber(),
                result.baseRevisionCode(),
                result.draftKey(),
                result.name(),
                result.status()
        );
    }

    private ReviewerSummaryResponse toReviewerSummaryResponse(ReviewerSummaryResult result) {
        return new ReviewerSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl(),
                result.reviewStatus(),
                result.reviewedAt()
        );
    }

    private FileItemResponse toFileItemResponse(IssueFileItemResult result) {
        return new FileItemResponse(
                result.fileId(),
                result.originalName(),
                result.contentType(),
                result.fileSize(),
                result.fileUrl(),
                result.createdAt()
        );
    }

    private FileItemResponse toFileItemResponse(AttachedFileResult result) {
        return new FileItemResponse(
                result.fileId(),
                result.originalName(),
                result.contentType(),
                result.fileSize(),
                result.fileUrl(),
                result.createdAt()
        );
    }

    private LinkedIssueBadgeResponse toLinkedIssueBadgeResponse(LinkedIssueBadgeResult result) {
        return new LinkedIssueBadgeResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state()
        );
    }
}
