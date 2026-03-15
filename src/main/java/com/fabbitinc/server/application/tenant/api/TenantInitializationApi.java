package com.fabbitinc.server.application.tenant.api;

import com.fabbitinc.server.application.label.api.LabelApi;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowPolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TenantInitializationApi {

    private final PartRevisionWorkflowPolicyApi partRevisionWorkflowPolicyApi;
    private final LabelApi labelApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeTenantDefaults() {
        partRevisionWorkflowPolicyApi.ensureDefaultPolicyExists();
        labelApi.ensureDefaultLabelsExist();
    }
}
