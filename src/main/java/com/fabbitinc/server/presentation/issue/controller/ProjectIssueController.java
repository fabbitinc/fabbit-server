package com.fabbitinc.server.presentation.issue.controller;

import com.fabbitinc.server.application.issue.query.IssueQuery;
import com.fabbitinc.server.application.issue.query.condition.ProjectIssueListCondition;
import com.fabbitinc.server.application.issue.query.result.IssueListResult;
import com.fabbitinc.server.application.issue.query.result.LabelBadgeResult;
import com.fabbitinc.server.application.issue.query.result.PartBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueListResponse;
import com.fabbitinc.server.presentation.issue.dto.response.IssueSummaryResponse;
import com.fabbitinc.server.presentation.issue.dto.response.LabelBadgeResponse;
import com.fabbitinc.server.presentation.issue.dto.response.PartBadgeResponse;
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
@RequestMapping("/api/v1/projects/{projectId}/issues")
@Tag(name = "projects", description = "프로젝트 관리 API")
public class ProjectIssueController {

    private final IssueQuery issueQuery;

    @Operation(
            summary = "프로젝트에 연결된 이슈 목록을 조회합니다",
            description = "프로젝트에 연결된 부품을 기준으로 연관 이슈 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public IssueListResponse listProjectIssues(
            @Parameter(description = "이슈 목록을 조회할 프로젝트 ID")
            @PathVariable UUID projectId
    ) {
        return toIssueListResponse(issueQuery.listProjectIssues(new ProjectIssueListCondition(projectId)));
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
}
