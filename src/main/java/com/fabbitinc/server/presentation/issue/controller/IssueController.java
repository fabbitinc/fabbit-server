package com.fabbitinc.server.presentation.issue.controller;

import com.fabbitinc.server.application.issue.query.IssueQuery;
import com.fabbitinc.server.application.issue.query.condition.IssueDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueListCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueTimelineCondition;
import com.fabbitinc.server.application.issue.query.result.EcPrefillResult;
import com.fabbitinc.server.application.issue.query.result.IssueDetailResult;
import com.fabbitinc.server.application.issue.query.result.IssueListResult;
import com.fabbitinc.server.application.issue.query.result.IssueLookupResult;
import com.fabbitinc.server.application.issue.query.result.LabelBadgeResult;
import com.fabbitinc.server.application.issue.query.result.LinkedEngineeringChangeSummaryResult;
import com.fabbitinc.server.application.issue.query.result.PartBadgeResult;
import com.fabbitinc.server.application.issue.usecase.AddIssueFilesUseCase;
import com.fabbitinc.server.application.issue.usecase.CloseIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.DeleteIssueFileUseCase;
import com.fabbitinc.server.application.issue.usecase.ReopenIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncLabelsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncLinkedEngineeringChangesUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncPartsUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncTeamAssigneesUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateCommentUseCase;
import com.fabbitinc.server.application.issue.usecase.UpdateIssueUseCase;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineDetailResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineRefResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineValueChangeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.application.workitem.usecase.result.AttachedFileResult;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import com.fabbitinc.server.application.workitem.usecase.result.CommentUserSummaryResult;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.issue.dto.request.CreateIssueRequest;
import com.fabbitinc.server.presentation.issue.dto.request.SyncAssigneesRequest;
import com.fabbitinc.server.presentation.issue.dto.request.SyncLabelsRequest;
import com.fabbitinc.server.presentation.issue.dto.request.SyncLinkedEngineeringChangesRequest;
import com.fabbitinc.server.presentation.issue.dto.request.SyncPartsRequest;
import com.fabbitinc.server.presentation.issue.dto.request.SyncTeamAssigneesRequest;
import com.fabbitinc.server.presentation.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.presentation.issue.dto.response.EcPrefillResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueListResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueLookupItemResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueLookupResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueSummaryResponse;
import com.fabbitinc.server.presentation.issue.dto.response.LabelBadgeResponse;
import com.fabbitinc.server.presentation.issue.dto.response.LinkedEngineeringChangeSummaryResponse;
import com.fabbitinc.server.presentation.issue.dto.response.PartBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.request.AttachFilesRequest;
import com.fabbitinc.server.presentation.workitem.dto.request.CreateCommentRequest;
import com.fabbitinc.server.presentation.workitem.dto.request.UpdateCommentRequest;
import com.fabbitinc.server.presentation.workitem.dto.response.CommentResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.SyncDiffResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineDetailResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineItemType;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineRefResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineValueChangeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/v1/issues")
@Tag(name = "issues", description = "이슈 조회/생성/수정/연결 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class IssueController {

    private final IssueQuery issueQuery;
    private final CreateIssueUseCase createIssueUseCase;
    private final UpdateIssueUseCase updateIssueUseCase;
    private final CloseIssueUseCase closeIssueUseCase;
    private final ReopenIssueUseCase reopenIssueUseCase;
    private final SyncAssigneesUseCase syncAssigneesUseCase;
    private final SyncTeamAssigneesUseCase syncTeamAssigneesUseCase;
    private final SyncLinkedEngineeringChangesUseCase syncLinkedEngineeringChangesUseCase;
    private final SyncLabelsUseCase syncLabelsUseCase;
    private final SyncPartsUseCase syncPartsUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final AddIssueFilesUseCase addIssueFilesUseCase;
    private final DeleteIssueFileUseCase deleteIssueFileUseCase;

    @Operation(
            operationId = "issueLookup",
            summary = "이슈 연결 picker UI용 경량 목록을 조회합니다",
            description = "이슈 연결 picker UI용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public IssueLookupResponse lookupIssues(
            @Parameter(description = "이슈 제목 검색어", example = "BOM")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toIssueLookupResponse(issueQuery.lookupIssues(new IssueLookupCondition(search, limit)));
    }

    @Operation(
            operationId = "issueList",
            summary = "이슈 목록을 조회합니다",
            description = "이슈 목록을 조회합니다"
    )
    @GetMapping
    public IssueListResponse listIssues(
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "이슈 상태 필터", example = "OPEN")
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toIssueListResponse(issueQuery.listIssues(new IssueListCondition(search, state, offset, limit)));
    }

    @Operation(
            operationId = "issueGet",
            summary = "이슈 상세 정보를 조회합니다",
            description = "이슈 ID로 상세 정보를 조회합니다"
    )
    @GetMapping("/{issueId}")
    public IssueResponse getIssue(
            @Parameter(description = "조회할 이슈 ID")
            @PathVariable UUID issueId
    ) {
        return toIssueResponse(issueQuery.getIssue(new IssueDetailCondition(issueId)));
    }

    @Operation(
            operationId = "issueGetEcPrefill",
            summary = "이슈 기반 EC 사전 입력 데이터를 조회합니다",
            description = "이슈에 연결된 부품 정보를 기반으로 EC 생성 시 사전 입력할 제목, 영향 항목 후보, 추천 검토자, 영향 분석 요약을 반환합니다"
    )
    @GetMapping("/{issueId}/ec-prefill")
    public EcPrefillResponse getEcPrefill(
            @Parameter(description = "이슈 ID")
            @PathVariable UUID issueId
    ) {
        EcPrefillResult result = issueQuery.getEcPrefill(issueId);
        return new EcPrefillResponse(
                result.suggestedTitle(),
                result.affectedItems().stream()
                        .map(item -> new EcPrefillResponse.AffectedItemSuggestionResponse(
                                item.partId(),
                                item.partNumber(),
                                item.revisionId(),
                                item.revisionCode()
                        ))
                        .toList(),
                result.suggestedReviewerIds(),
                new EcPrefillResponse.ImpactSummaryResponse(
                        result.impactSummary().affectedBomCount(),
                        result.impactSummary().affectedProjectCount(),
                        result.impactSummary().draftRevisionCount()
                )
        );
    }

    @Operation(
            operationId = "issueCreate",
            summary = "이슈를 생성하고 연관 정보(부품/담당자/라벨/파일)를 일괄 연결합니다",
            description = "이슈를 생성하고 연관 정보(부품/담당자/라벨/파일)를 일괄 연결합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(
            @Parameter(description = "이슈 생성 요청")
            @Valid @RequestBody CreateIssueRequest request
    ) {
        CreateIssueUseCase.CreateIssueResult result = createIssueUseCase.execute(
                new CreateIssueUseCase.CreateIssueCommand(
                        request.title(),
                        request.body(),
                        request.partIds(),
                        request.assigneeUserIds(),
                        request.teamAssigneeIds(),
                        request.labelIds(),
                        request.fileIds()
                )
        );
        return toIssueResponse(issueQuery.getIssue(new IssueDetailCondition(result.issueId())));
    }

    @Operation(
            operationId = "issueUpdate",
            summary = "이슈 제목/본문을 수정합니다",
            description = "이슈 제목/본문을 수정합니다"
    )
    @PatchMapping("/{issueId}")
    public IssueResponse updateIssue(
            @Parameter(description = "수정할 이슈 ID")
            @PathVariable UUID issueId,
            @Parameter(description = "이슈 수정 요청")
            @Valid @RequestBody UpdateIssueRequest request
    ) {
        updateIssueUseCase.execute(new UpdateIssueUseCase.UpdateIssueCommand(issueId, request.title(), request.body()));
        return toIssueResponse(issueQuery.getIssue(new IssueDetailCondition(issueId)));
    }

    @Operation(
            operationId = "issueSyncAssignees",
            summary = "개인 담당자 목록을 동기화합니다",
            description = "개인 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueId}/assignees")
    public SyncDiffResponse syncAssignees(
            @PathVariable UUID issueId,
            @Valid @RequestBody SyncAssigneesRequest request
    ) {
        return toSyncDiffResponse(
                syncAssigneesUseCase.execute(
                        new SyncAssigneesUseCase.SyncAssigneesCommand(issueId, request.userIds())
                )
        );
    }

    @Operation(
            operationId = "issueSyncTeamAssignees",
            summary = "팀 담당자 목록을 동기화합니다",
            description = "팀 담당자 목록을 동기화합니다"
    )
    @PutMapping("/{issueId}/assigned-teams")
    public SyncDiffResponse syncTeamAssignees(
            @PathVariable UUID issueId,
            @Valid @RequestBody SyncTeamAssigneesRequest request
    ) {
        return toSyncDiffResponse(
                syncTeamAssigneesUseCase.execute(
                        new SyncTeamAssigneesUseCase.SyncTeamAssigneesCommand(issueId, request.teamIds())
                )
        );
    }

    @Operation(
            operationId = "issueSyncLinkedEngineeringChanges",
            summary = "이슈에 연결된 변경관리 목록을 동기화합니다",
            description = "이슈에 연결된 변경관리 목록을 동기화합니다"
    )
    @PutMapping("/{issueId}/engineering-changes")
    public SyncDiffResponse syncLinkedEngineeringChanges(
            @PathVariable UUID issueId,
            @Valid @RequestBody SyncLinkedEngineeringChangesRequest request
    ) {
        return toSyncDiffResponse(
                syncLinkedEngineeringChangesUseCase.execute(
                        new SyncLinkedEngineeringChangesUseCase.SyncLinkedEngineeringChangesCommand(
                                issueId,
                                request.engineeringChangeIds()
                        )
                )
        );
    }

    @Operation(
            operationId = "issueClose",
            summary = "이슈를 닫습니다 (OPEN -> CLOSED)",
            description = "이슈를 닫습니다 (OPEN -> CLOSED)"
    )
    @PostMapping("/{issueId}/close")
    public IssueResponse closeIssue(
            @PathVariable UUID issueId
    ) {
        closeIssueUseCase.execute(new CloseIssueUseCase.CloseIssueCommand(issueId));
        return toIssueResponse(issueQuery.getIssue(new IssueDetailCondition(issueId)));
    }

    @Operation(
            operationId = "issueReopen",
            summary = "이슈를 다시 엽니다 (CLOSED -> OPEN)",
            description = "이슈를 다시 엽니다 (CLOSED -> OPEN)"
    )
    @PostMapping("/{issueId}/reopen")
    public IssueResponse reopenIssue(
            @PathVariable UUID issueId
    ) {
        reopenIssueUseCase.execute(new ReopenIssueUseCase.ReopenIssueCommand(issueId));
        return toIssueResponse(issueQuery.getIssue(new IssueDetailCondition(issueId)));
    }

    @Operation(
            operationId = "issueSyncLabels",
            summary = "라벨 목록을 동기화합니다",
            description = "라벨 목록을 동기화합니다"
    )
    @PutMapping("/{issueId}/labels")
    public SyncDiffResponse syncLabels(
            @PathVariable UUID issueId,
            @Valid @RequestBody SyncLabelsRequest request
    ) {
        return toSyncDiffResponse(
                syncLabelsUseCase.execute(
                        new SyncLabelsUseCase.SyncLabelsCommand(issueId, request.labelIds())
                )
        );
    }

    @Operation(
            operationId = "issueSyncParts",
            summary = "부품 목록을 동기화합니다",
            description = "부품 목록을 동기화합니다"
    )
    @PutMapping("/{issueId}/parts")
    public SyncDiffResponse syncParts(
            @PathVariable UUID issueId,
            @Valid @RequestBody SyncPartsRequest request
    ) {
        return toSyncDiffResponse(
                syncPartsUseCase.execute(
                        new SyncPartsUseCase.SyncPartsCommand(issueId, request.partIds())
                )
        );
    }

    @Operation(
            operationId = "issueGetTimeline",
            summary = "댓글과 활동 이력을 시간순으로 병합 조회합니다",
            description = "댓글과 활동 이력을 시간순으로 병합 조회합니다"
    )
    @GetMapping("/{issueId}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable UUID issueId
    ) {
        return toTimelineResponse(issueQuery.getTimeline(new IssueTimelineCondition(issueId)));
    }

    @Operation(
            operationId = "issueCreateComment",
            summary = "댓글을 생성합니다",
            description = "댓글을 생성합니다"
    )
    @PostMapping("/{issueId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable UUID issueId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return toCommentResponse(
                createCommentUseCase.execute(
                        new CreateCommentUseCase.CreateCommentCommand(issueId, request.body())
                )
        );
    }

    @Operation(
            operationId = "issueUpdateComment",
            summary = "댓글을 수정합니다",
            description = "댓글을 수정합니다"
    )
    @PatchMapping("/{issueId}/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return toCommentResponse(
                updateCommentUseCase.execute(
                        new UpdateCommentUseCase.UpdateCommentCommand(issueId, commentId, request.body())
                )
        );
    }

    @Operation(
            operationId = "issueDeleteComment",
            summary = "댓글을 삭제합니다",
            description = "댓글을 삭제합니다"
    )
    @DeleteMapping("/{issueId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId
    ) {
        deleteCommentUseCase.execute(new DeleteCommentUseCase.DeleteCommentCommand(issueId, commentId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "issueAddFiles",
            summary = "첨부파일을 배치 연결합니다",
            description = "첨부파일을 배치 연결합니다"
    )
    @PostMapping("/{issueId}/files")
    public List<FileItemResponse> addFiles(
            @PathVariable UUID issueId,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        return addIssueFilesUseCase.execute(
                        new AddIssueFilesUseCase.AddIssueFilesCommand(issueId, request.fileIds())
                )
                .stream()
                .map(this::toFileItemResponse)
                .toList();
    }

    @Operation(
            operationId = "issueDeleteFile",
            summary = "첨부파일 1건을 삭제(soft delete)합니다",
            description = "첨부파일 1건을 삭제(soft delete)합니다"
    )
    @DeleteMapping("/{issueId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID issueId,
            @PathVariable UUID fileId
    ) {
        deleteIssueFileUseCase.execute(new DeleteIssueFileUseCase.DeleteIssueFileCommand(issueId, fileId));
        return ResponseEntity.noContent().build();
    }

    private SyncDiffResponse toSyncDiffResponse(SyncDiffResult result) {
        return new SyncDiffResponse(result.addedCount(), result.removedCount());
    }

    private CommentResponse toCommentResponse(CommentResult result) {
        return new CommentResponse(
                result.id(),
                result.targetId(),
                result.body(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                toUserSummaryResponse(result.createdBy())
        );
    }

    private IssueLookupResponse toIssueLookupResponse(IssueLookupResult result) {
        return new IssueLookupResponse(
                result.items().stream()
                        .map(item -> new IssueLookupItemResponse(
                                item.id(),
                                item.number(),
                                item.title(),
                                item.state()
                        ))
                        .toList()
        );
    }

    private IssueListResponse toIssueListResponse(IssueListResult result) {
        return new IssueListResponse(
                result.openCount(),
                result.closedCount(),
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream().map(this::toIssueSummaryResponse).toList()
        );
    }

    private IssueSummaryResponse toIssueSummaryResponse(IssueListResult.Item result) {
        return new IssueSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                toUserSummaryResponse(result.createdBy()),
                result.labels().stream().map(this::toLabelBadgeResponse).toList(),
                result.assignees().stream().map(this::toUserSummaryResponse).toList(),
                result.assignedTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.parts().stream().map(this::toPartBadgeResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount()
        );
    }

    private IssueResponse toIssueResponse(IssueDetailResult result) {
        return new IssueResponse(
                result.id(),
                result.number(),
                result.title(),
                result.body(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                toUserSummaryResponse(result.createdBy()),
                result.labels().stream().map(this::toLabelBadgeResponse).toList(),
                result.assignees().stream().map(this::toUserSummaryResponse).toList(),
                result.assignedTeams().stream().map(this::toTeamBadgeResponse).toList(),
                result.parts().stream().map(this::toPartBadgeResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.linkedEngineeringChanges().stream().map(this::toLinkedEngineeringChangeSummaryResponse).toList()
        );
    }

    private TimelineResponse toTimelineResponse(TimelineResult result) {
        return new TimelineResponse(
                result.items().stream().map(this::toTimelineItemResponse).toList()
        );
    }

    private TimelineItemResponse toTimelineItemResponse(TimelineResult.Item result) {
        return new TimelineItemResponse(
                toTimelineItemType(result.type()),
                result.id(),
                result.action(),
                result.scope(),
                toUserSummaryResponse(result.actor()),
                toTimelineDetailResponse(result.detail()),
                result.body(),
                toUserSummaryResponse(result.author()),
                result.createdAt(),
                result.updatedAt(),
                result.isModified()
        );
    }

    private TimelineDetailResponse toTimelineDetailResponse(TimelineDetailResult result) {
        if (result == null) {
            return null;
        }
        return new TimelineDetailResponse(
                result.changes().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> toTimelineValueChangeResponse(entry.getValue()),
                                (left, right) -> right,
                                java.util.LinkedHashMap::new
                        )),
                result.refs().stream().map(this::toTimelineRefResponse).toList(),
                result.added().stream().map(this::toTimelineRefResponse).toList(),
                result.removed().stream().map(this::toTimelineRefResponse).toList()
        );
    }

    private TimelineValueChangeResponse toTimelineValueChangeResponse(TimelineValueChangeResult result) {
        return new TimelineValueChangeResponse(result.oldValue(), result.newValue());
    }

    private TimelineRefResponse toTimelineRefResponse(TimelineRefResult result) {
        return new TimelineRefResponse(result.id(), result.type(), result.label(), result.meta());
    }

    private TimelineItemType toTimelineItemType(TimelineItemTypeResult result) {
        if (result == TimelineItemTypeResult.ACTIVITY) {
            return TimelineItemType.ACTIVITY;
        }
        return TimelineItemType.COMMENT;
    }

    private UserSummaryResponse toUserSummaryResponse(UserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new UserSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private UserSummaryResponse toUserSummaryResponse(CommentUserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new UserSummaryResponse(
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

    private FileItemResponse toFileItemResponse(FileItemResult result) {
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

    private LinkedEngineeringChangeSummaryResponse toLinkedEngineeringChangeSummaryResponse(LinkedEngineeringChangeSummaryResult result) {
        return new LinkedEngineeringChangeSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state()
        );
    }
}
