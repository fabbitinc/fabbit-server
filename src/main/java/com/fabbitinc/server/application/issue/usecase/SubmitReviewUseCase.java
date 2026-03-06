package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.usecase.result.SubmitReviewResult;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import com.fabbitinc.server.domain.issue.model.ChangeRequestReviewer;
import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SubmitReviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SubmitReviewResult execute(SubmitReviewCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(command.issueNumber());

        ChangeRequestReviewer reviewer = issueService.submitReview(auth.userId(), changeRequest.getId(), command.status());
        return new SubmitReviewResult(reviewer.getReviewStatus(), reviewer.getReviewedAt());
    }

    public record SubmitReviewCommand(
            int issueNumber,
            ReviewStatus status
    ) {
    }
}
