package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.usecase.result.SubmitReviewResult;
import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeReviewer;
import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SubmitReviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public SubmitReviewResult execute(SubmitReviewCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.issueNumber());

        EngineeringChangeReviewer reviewer =
                engineeringChangeService.submitReview(auth.userId(), engineeringChange.getId(), command.status());
        return new SubmitReviewResult(reviewer.getReviewStatus(), reviewer.getReviewedAt());
    }

    public record SubmitReviewCommand(
            int issueNumber,
            ReviewStatus status
    ) {
    }
}
