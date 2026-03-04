package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.issue.dto.request.CreateChangeRequestRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateChangeRequestUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;
    private final IssueService issueService;

    @Transactional
    public int execute(String authorizationHeader, CreateChangeRequestRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        ChangeRequest changeRequest = issueService.createChangeRequest(auth.userId(), request.title(), request.body());

        if (request.issueNumber() != null) {
            Issue issue = issueService.getIssueByNumberOrThrow(request.issueNumber());
            issueService.syncIssues(auth.userId(), changeRequest.getId(), java.util.List.of(issue.getId()), false);
        }
        if (!request.partIds().isEmpty()) {
            issueService.syncParts(auth.userId(), changeRequest.getId(), request.partIds(), false);
        }
        if (!request.assigneeUserIds().isEmpty()) {
            issueService.syncAssignees(auth.userId(), changeRequest.getId(), request.assigneeUserIds(), false);
        }
        if (!request.teamAssigneeIds().isEmpty()) {
            issueService.syncTeamAssignees(changeRequest.getId(), request.teamAssigneeIds());
        }
        if (!request.labelIds().isEmpty()) {
            issueService.syncLabels(auth.userId(), changeRequest.getId(), request.labelIds(), false);
        }
        if (!request.fileIds().isEmpty()) {
            issueService.attachFiles(
                    auth.userId(),
                    changeRequest.getId(),
                    fileService.validateAttachable(request.fileIds()),
                    false
            );
        }
        if (!request.reviewerUserIds().isEmpty()) {
            issueService.syncReviewers(auth.userId(), changeRequest.getId(), request.reviewerUserIds(), false);
        }
        if (!request.teamReviewerIds().isEmpty()) {
            issueService.syncTeamReviewers(changeRequest.getId(), request.teamReviewerIds());
        }

        return changeRequest.getNumber();
    }
}
