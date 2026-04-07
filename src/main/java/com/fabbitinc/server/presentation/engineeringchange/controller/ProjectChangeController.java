package com.fabbitinc.server.presentation.engineeringchange.controller;

import com.fabbitinc.server.application.engineeringchange.query.EngineeringChangeQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.ProjectChangeListCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeListResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeStepResult;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeListResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.LabelBadgeResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeStepResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeSummaryResponse;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/changes")
@Tag(name = "projects", description = "프로젝트 관리 API")
public class ProjectChangeController {

    private final EngineeringChangeQuery engineeringChangeQuery;

    @Operation(
            operationId = "projectChangeList",
            summary = "프로젝트에 연결된 변경관리 목록을 조회합니다",
            description = "프로젝트에 연결된 부품과 이슈를 기준으로 연관 변경관리 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public EngineeringChangeListResponse listProjectChanges(
            @Parameter(description = "변경관리 목록을 조회할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        return toEngineeringChangeListResponse(
                engineeringChangeQuery.listProjectChanges(new ProjectChangeListCondition(projectId))
        );
    }

    private EngineeringChangeListResponse toEngineeringChangeListResponse(EngineeringChangeListResult result) {
        return new EngineeringChangeListResponse(
                result.openCount(),
                result.progressCount(),
                result.doneCount(),
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream().map(this::toEngineeringChangeSummaryResponse).toList()
        );
    }

    private EngineeringChangeSummaryResponse toEngineeringChangeSummaryResponse(EngineeringChangeListResult.Item result) {
        return new EngineeringChangeSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                toUserSummaryResponse(result.createdBy()),
                result.labels().stream().map(this::toLabelBadgeResponse).toList(),
                result.steps().stream().map(this::toEngineeringChangeStepResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.releasedAt(),
                toUserSummaryResponse(result.releasedBy())
        );
    }

    private LabelBadgeResponse toLabelBadgeResponse(com.fabbitinc.server.application.engineeringchange.query.result.LabelBadgeResult result) {
        return new LabelBadgeResponse(result.id(), result.name(), result.color());
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
}
