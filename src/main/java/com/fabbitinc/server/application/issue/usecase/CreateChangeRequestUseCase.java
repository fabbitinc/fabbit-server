package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.part.api.ChangeRequestPartRevisionRef;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
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
public class CreateChangeRequestUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final IssueService issueService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public CreateChangeRequestResult execute(CreateChangeRequestCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        ChangeRequest changeRequest = issueService.createChangeRequest(auth.userId(), command.title(), command.body());

        if (command.issueNumber() != null) {
            Issue issue = issueService.getIssueByNumberOrThrow(command.issueNumber());
            issueService.syncIssues(auth.userId(), changeRequest.getId(), java.util.List.of(issue.getId()), false);
        }
        if (!command.partIds().isEmpty()) {
            issueService.syncParts(auth.userId(), changeRequest.getId(), command.partIds(), false);
        }
        if (!command.partRevisions().isEmpty()) {
            partRevisionWorkflowApi.syncChangeRequestPartRevisions(
                    changeRequest.getId(),
                    command.partRevisions().stream()
                            .map(item -> new ChangeRequestPartRevisionRef(
                                    item.partNumber(),
                                    item.baseRevisionCode(),
                                    item.draftKey()
                            ))
                            .toList()
            );
        }
        if (!command.assigneeUserIds().isEmpty()) {
            issueService.syncAssignees(auth.userId(), changeRequest.getId(), command.assigneeUserIds(), false);
        }
        if (!command.teamAssigneeIds().isEmpty()) {
            issueService.syncTeamAssignees(changeRequest.getId(), command.teamAssigneeIds());
        }
        if (!command.labelIds().isEmpty()) {
            issueService.syncLabels(auth.userId(), changeRequest.getId(), command.labelIds(), false);
        }
        if (!command.fileIds().isEmpty()) {
            issueService.attachFiles(
                    auth.userId(),
                    changeRequest.getId(),
                    fileService.validateAttachable(command.fileIds()),
                    false
            );
        }
        if (!command.reviewerUserIds().isEmpty()) {
            issueService.syncReviewers(auth.userId(), changeRequest.getId(), command.reviewerUserIds(), false);
        }
        if (!command.teamReviewerIds().isEmpty()) {
            issueService.syncTeamReviewers(changeRequest.getId(), command.teamReviewerIds());
        }

        return new CreateChangeRequestResult(changeRequest.getNumber());
    }

    public record CreateChangeRequestCommand(
            String title,
            JsonNode body,
            Integer issueNumber,
            List<UUID> partIds,
            List<PartRevisionTarget> partRevisions,
            List<UUID> assigneeUserIds,
            List<UUID> teamAssigneeIds,
            List<UUID> labelIds,
            List<UUID> fileIds,
            List<UUID> reviewerUserIds,
            List<UUID> teamReviewerIds
    ) {
        public CreateChangeRequestCommand {
            partIds = partIds == null ? List.of() : List.copyOf(partIds);
            partRevisions = partRevisions == null ? List.of() : List.copyOf(partRevisions);
            assigneeUserIds = assigneeUserIds == null ? List.of() : List.copyOf(assigneeUserIds);
            teamAssigneeIds = teamAssigneeIds == null ? List.of() : List.copyOf(teamAssigneeIds);
            labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
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

    public record CreateChangeRequestResult(int issueNumber) {
    }
}
