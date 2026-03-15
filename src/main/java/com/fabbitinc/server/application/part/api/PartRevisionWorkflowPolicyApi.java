package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PartRevisionWorkflowPolicyApi {

    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;

    @Transactional
    public void ensureDefaultPolicyExists() {
        partRevisionWorkflowPolicyService.ensureDefaultPolicyExists();
    }
}
