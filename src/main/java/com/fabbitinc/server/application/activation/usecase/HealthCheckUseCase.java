package com.fabbitinc.server.application.activation.usecase;

import com.fabbitinc.server.application.activation.service.ActivationService;
import com.fabbitinc.server.application.activation.service.output.HealthCheckOutput;
import com.fabbitinc.server.application.activation.usecase.result.HealthCheckIssueResult;
import com.fabbitinc.server.application.activation.usecase.result.HealthCheckResult;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthCheckUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ActivationService activationService;

    public HealthCheckResult execute() {
        currentAuthProvider.getCurrentAuth();
        HealthCheckOutput output = activationService.healthCheck();
        return new HealthCheckResult(
                output.totalNodes(),
                output.totalRelationships(),
                output.nodeCounts(),
                output.relationshipCounts(),
                output.issues().stream()
                        .map(issue -> new HealthCheckIssueResult(
                                issue.category(),
                                issue.severity(),
                                issue.message(),
                                issue.count()
                        ))
                        .toList()
        );
    }
}
