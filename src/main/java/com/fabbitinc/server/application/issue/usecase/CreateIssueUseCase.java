package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateIssueUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final IssueService issueService;

    public CreateIssueResult execute(CreateIssueCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        Issue issue = issueService.createIssue(auth.userId(), command.title(), command.body());

        if (!command.partIds().isEmpty()) {
            issueService.syncParts(auth.userId(), issue.getId(), command.partIds(), false);
        }
        if (!command.assigneeUserIds().isEmpty()) {
            issueService.syncAssignees(auth.userId(), issue.getId(), command.assigneeUserIds(), false);
        }
        if (!command.teamAssigneeIds().isEmpty()) {
            issueService.syncTeamAssignees(issue.getId(), command.teamAssigneeIds());
        }
        if (!command.labelIds().isEmpty()) {
            issueService.syncLabels(auth.userId(), issue.getId(), command.labelIds(), false);
        }
        if (!command.fileIds().isEmpty()) {
            issueService.attachFiles(
                    auth.userId(),
                    issue.getId(),
                    fileService.validateAttachable(command.fileIds()),
                    false
            );
        }

        return new CreateIssueResult(issue.getNumber());
    }

    public record CreateIssueCommand(
            String title,
            JsonNode body,
            List<UUID> partIds,
            List<UUID> assigneeUserIds,
            List<UUID> teamAssigneeIds,
            List<UUID> labelIds,
            List<UUID> fileIds
    ) {
        public CreateIssueCommand {
            partIds = partIds == null ? List.of() : List.copyOf(partIds);
            assigneeUserIds = assigneeUserIds == null ? List.of() : List.copyOf(assigneeUserIds);
            teamAssigneeIds = teamAssigneeIds == null ? List.of() : List.copyOf(teamAssigneeIds);
            labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
        }
    }

    public record CreateIssueResult(int issueNumber) {
    }
}
