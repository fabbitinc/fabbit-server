package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionRef;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import com.fabbitinc.server.domain.issue.model.Issue;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public CreateEngineeringChangeResult execute(CreateEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange =
                engineeringChangeService.createEngineeringChange(auth.userId(), command.title(), command.body());

        if (command.issueNumber() != null) {
            Issue issue = engineeringChangeService.getIssueByNumberOrThrow(command.issueNumber());
            engineeringChangeService.syncIssues(
                    auth.userId(),
                    engineeringChange.getId(),
                    java.util.List.of(issue.getId()),
                    false
            );
        }
        if (!command.partRevisions().isEmpty()) {
            partRevisionWorkflowApi.syncEngineeringChangePartRevisions(
                    engineeringChange.getId(),
                    command.partRevisions().stream()
                            .map(item -> new EngineeringChangePartRevisionRef(
                                    item.partNumber(),
                                    item.baseRevisionCode(),
                                    item.draftKey()
                            ))
                            .toList()
            );
        }
        if (!command.fileIds().isEmpty()) {
            engineeringChangeService.attachFiles(
                    auth.userId(),
                    engineeringChange.getId(),
                    fileService.validateAttachable(command.fileIds()),
                    false
            );
        }
        if (!command.reviewerUserIds().isEmpty()) {
            engineeringChangeService.syncReviewers(
                    auth.userId(),
                    engineeringChange.getId(),
                    command.reviewerUserIds(),
                    false
            );
        }
        if (!command.teamReviewerIds().isEmpty()) {
            engineeringChangeService.syncTeamReviewers(engineeringChange.getId(), command.teamReviewerIds());
        }

        return new CreateEngineeringChangeResult(engineeringChange.getNumber());
    }

    public record CreateEngineeringChangeCommand(
            String title,
            JsonNode body,
            Integer issueNumber,
            List<PartRevisionTarget> partRevisions,
            List<UUID> fileIds,
            List<UUID> reviewerUserIds,
            List<UUID> teamReviewerIds
    ) {
        public CreateEngineeringChangeCommand {
            partRevisions = partRevisions == null ? List.of() : List.copyOf(partRevisions);
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
            reviewerUserIds = reviewerUserIds == null ? List.of() : List.copyOf(reviewerUserIds);
            teamReviewerIds = teamReviewerIds == null ? List.of() : List.copyOf(teamReviewerIds);
        }

        public record PartRevisionTarget(
                String partNumber,
                String baseRevisionCode,
                String draftKey
        ) {
        }
    }

    public record CreateEngineeringChangeResult(int issueNumber) {
    }
}
