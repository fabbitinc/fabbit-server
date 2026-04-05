package com.fabbitinc.server.presentation.engineeringchange.controller;

import com.fabbitinc.server.application.engineeringchange.query.EngineeringChangeQuery;
import com.fabbitinc.server.application.engineeringchange.query.WorkflowTemplateQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeDetailCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeAffectedItemResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeDetailResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeStepResult;
import com.fabbitinc.server.application.engineeringchange.query.result.LinkedIssueSummaryResult;
import com.fabbitinc.server.application.engineeringchange.query.result.WorkflowTemplateResult;
import com.fabbitinc.server.application.engineeringchange.usecase.ApplyWorkflowTemplateUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateWorkflowTemplateUseCase;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.CreateWorkflowTemplateRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeAffectedItemResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeStepResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.LinkedIssueSummaryResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.WorkflowTemplateResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.WorkflowTemplateStageResponse;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workflow-templates")
@Tag(name = "workflow-templates", description = "워크플로우 템플릿 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class WorkflowTemplateController {

    private final CreateWorkflowTemplateUseCase createWorkflowTemplateUseCase;
    private final ApplyWorkflowTemplateUseCase applyWorkflowTemplateUseCase;
    private final WorkflowTemplateQuery workflowTemplateQuery;
    private final EngineeringChangeQuery engineeringChangeQuery;

    @Operation(
            operationId = "workflowTemplateCreate",
            summary = "워크플로우 템플릿을 생성합니다",
            description = "재사용 가능한 워크플로우 템플릿을 생성합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowTemplateResponse createWorkflowTemplate(
            @Parameter(description = "워크플로우 템플릿 생성 요청")
            @Valid @RequestBody CreateWorkflowTemplateRequest request
    ) {
        CreateWorkflowTemplateUseCase.CreateWorkflowTemplateResult result = createWorkflowTemplateUseCase.execute(
                new CreateWorkflowTemplateUseCase.CreateWorkflowTemplateCommand(
                        request.name(),
                        request.description(),
                        request.stages().stream()
                                .map(stage -> new CreateWorkflowTemplateUseCase.CreateWorkflowTemplateCommand.StageItem(
                                        stage.stepType(),
                                        stage.sequence(),
                                        stage.completionPolicy(),
                                        stage.minApprovals(),
                                        stage.assignees().stream()
                                                .map(a -> new CreateWorkflowTemplateUseCase.CreateWorkflowTemplateCommand.AssigneeItem(
                                                        a.assigneeType(),
                                                        a.assigneeId()
                                                ))
                                                .toList()
                                ))
                                .toList()
                )
        );
        // 생성 후 query를 통해 결과를 조회하여 반환
        return workflowTemplateQuery.listWorkflowTemplates().stream()
                .filter(t -> t.id().equals(result.workflowTemplateId()))
                .findFirst()
                .map(this::toWorkflowTemplateResponse)
                .orElseThrow();
    }

    @Operation(
            operationId = "workflowTemplateList",
            summary = "워크플로우 템플릿 목록을 조회합니다",
            description = "생성일 역순으로 모든 워크플로우 템플릿 목록을 조회합니다"
    )
    @GetMapping
    public List<WorkflowTemplateResponse> listWorkflowTemplates() {
        return workflowTemplateQuery.listWorkflowTemplates().stream()
                .map(this::toWorkflowTemplateResponse)
                .toList();
    }

    @Operation(
            operationId = "workflowTemplateApply",
            summary = "워크플로우 템플릿을 변경관리에 적용합니다",
            description = "선택한 워크플로우 템플릿의 단계 구성을 변경관리에 일괄 적용합니다"
    )
    @PostMapping("/{templateId}/apply/{engineeringChangeId}")
    public EngineeringChangeResponse applyWorkflowTemplate(
            @Parameter(description = "적용할 워크플로우 템플릿 ID")
            @PathVariable UUID templateId,
            @Parameter(description = "대상 변경관리 ID")
            @PathVariable UUID engineeringChangeId
    ) {
        ApplyWorkflowTemplateUseCase.ApplyWorkflowTemplateResult result = applyWorkflowTemplateUseCase.execute(
                new ApplyWorkflowTemplateUseCase.ApplyWorkflowTemplateCommand(engineeringChangeId, templateId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(result.engineeringChangeId()))
        );
    }

    private WorkflowTemplateResponse toWorkflowTemplateResponse(WorkflowTemplateResult result) {
        return new WorkflowTemplateResponse(
                result.id(),
                result.name(),
                result.description(),
                result.stages().stream().map(this::toWorkflowTemplateStageResponse).toList(),
                result.createdAt()
        );
    }

    private WorkflowTemplateStageResponse toWorkflowTemplateStageResponse(WorkflowTemplateResult.StageResult result) {
        return new WorkflowTemplateStageResponse(
                result.stageId(),
                result.stepType(),
                result.sequence(),
                result.completionPolicy(),
                result.minApprovals(),
                result.assignees().stream()
                        .map(a -> new WorkflowTemplateStageResponse.AssigneeResponse(
                                a.assigneeId(),
                                a.assigneeType()
                        ))
                        .toList()
        );
    }

    private EngineeringChangeResponse toEngineeringChangeResponse(EngineeringChangeDetailResult result) {
        return new EngineeringChangeResponse(
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
                toLinkedIssueSummaryResponse(result.sourceIssue()),
                result.steps().stream().map(this::toEngineeringChangeStepResponse).toList(),
                result.affectedItems().stream().map(this::toAffectedItemResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.releasedAt(),
                toUserSummaryResponse(result.releasedBy()),
                result.linkedIssues().stream().map(this::toLinkedIssueSummaryResponse).toList()
        );
    }

    private EngineeringChangeStepResponse toEngineeringChangeStepResponse(EngineeringChangeStepResult result) {
        return new EngineeringChangeStepResponse(
                result.stepId(),
                result.stepType(),
                result.assigneeType(),
                result.sequence(),
                result.stepStageId(),
                result.completionPolicy(),
                result.deadline(),
                result.status(),
                toUserSummaryResponse(result.assigneeUser()),
                toTeamBadgeResponse(result.assigneeTeam()),
                toUserSummaryResponse(result.actedBy()),
                result.actedAt()
        );
    }

    private EngineeringChangeAffectedItemResponse toAffectedItemResponse(EngineeringChangeAffectedItemResult result) {
        return new EngineeringChangeAffectedItemResponse(
                result.id(),
                result.itemType(),
                result.targetId(),
                result.actionDetail(),
                result.partId(),
                result.partNumber(),
                result.revisionCode(),
                result.name(),
                result.status()
        );
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

    private TeamBadgeResponse toTeamBadgeResponse(TeamBadgeResult result) {
        if (result == null) {
            return null;
        }
        return new TeamBadgeResponse(result.id(), result.name());
    }

    private LinkedIssueSummaryResponse toLinkedIssueSummaryResponse(LinkedIssueSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new LinkedIssueSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state()
        );
    }
}
