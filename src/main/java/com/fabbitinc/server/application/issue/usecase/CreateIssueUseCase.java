package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.dto.request.CreateIssueRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateIssueUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;
    private final IssueService issueService;

    @Transactional
    public int execute(String authorizationHeader, CreateIssueRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        Issue issue = issueService.createIssue(auth.userId(), request.title(), request.body());

        if (!request.partIds().isEmpty()) {
            issueService.syncParts(auth.userId(), issue.getId(), request.partIds(), false);
        }
        if (!request.assigneeUserIds().isEmpty()) {
            issueService.syncAssignees(auth.userId(), issue.getId(), request.assigneeUserIds(), false);
        }
        if (!request.teamAssigneeIds().isEmpty()) {
            issueService.syncTeamAssignees(issue.getId(), request.teamAssigneeIds());
        }
        if (!request.labelIds().isEmpty()) {
            issueService.syncLabels(auth.userId(), issue.getId(), request.labelIds(), false);
        }
        if (!request.fileIds().isEmpty()) {
            issueService.attachFiles(
                    auth.userId(),
                    issue.getId(),
                    fileService.validateAttachable(request.fileIds()),
                    false
            );
        }

        return issue.getNumber();
    }
}
