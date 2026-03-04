package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.SubmitReviewRequest;
import com.fabbitinc.server.application.issue.dto.response.SubmitReviewResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import com.fabbitinc.server.domain.issue.model.ChangeRequestReviewer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubmitReviewUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public SubmitReviewResponse execute(String authorizationHeader, int issueNumber, SubmitReviewRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(issueNumber);

        ChangeRequestReviewer reviewer = issueService.submitReview(auth.userId(), changeRequest.getId(), request.status());
        return new SubmitReviewResponse(reviewer.getReviewStatus(), reviewer.getReviewedAt());
    }
}
