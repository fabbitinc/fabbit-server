package com.fabbitinc.server.application.settings.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.settings.query.result.SettingsResult;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import com.fabbitinc.server.domain.part.repository.PartRevisionWorkflowPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyRepository partRevisionWorkflowPolicyRepository;

    public SettingsResult get() {
        currentAuthProvider.getCurrentAuth();
        return new SettingsResult(
                partRevisionWorkflowPolicyRepository.findByPolicyKey(PartRevisionWorkflowPolicy.DEFAULT_POLICY_KEY)
                        .orElseThrow(() -> new AppException(
                                ErrorCode.NOT_FOUND,
                                "기본 리비전 워크플로 설정을 찾을 수 없습니다"
                        ))
                        .getMode()
        );
    }
}
