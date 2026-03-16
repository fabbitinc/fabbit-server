package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import com.fabbitinc.server.domain.part.repository.PartRevisionWorkflowPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartRevisionWorkflowPolicyService {

    private final PartRevisionWorkflowPolicyRepository partRevisionWorkflowPolicyRepository;

    public PartRevisionWorkflowPolicy getCurrent() {
        return partRevisionWorkflowPolicyRepository.findByPolicyKey(PartRevisionWorkflowPolicy.DEFAULT_POLICY_KEY)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "기본 리비전 워크플로 설정을 찾을 수 없습니다"
                ));
    }

    public PartRevisionWorkflowMode getCurrentMode() {
        return getCurrent().getMode();
    }

    public boolean requiresEngineeringChange() {
        return getCurrent().requiresEngineeringChange();
    }

    public PartRevisionWorkflowPolicy ensureDefaultPolicyExists() {
        return partRevisionWorkflowPolicyRepository.findByPolicyKey(PartRevisionWorkflowPolicy.DEFAULT_POLICY_KEY)
                .orElseGet(() -> partRevisionWorkflowPolicyRepository.save(PartRevisionWorkflowPolicy.createDefault()));
    }

    public void assertDirectModeEnabled() {
        if (requiresEngineeringChange()) {
            throw new AppException(
                    ErrorCode.PART_WORKFLOW_POLICY_FORBIDDEN,
                    "변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다"
            );
        }
    }

    public void assertEngineeringChangeModeEnabled() {
        if (!requiresEngineeringChange()) {
            throw new AppException(
                    ErrorCode.PART_WORKFLOW_POLICY_FORBIDDEN,
                    "직접 승인 모드에서는 변경관리 기반 리비전 워크플로를 사용할 수 없습니다"
            );
        }
    }

    public PartRevisionWorkflowPolicy changeMode(PartRevisionWorkflowMode mode) {
        PartRevisionWorkflowPolicy policy = partRevisionWorkflowPolicyRepository
                .findByPolicyKey(PartRevisionWorkflowPolicy.DEFAULT_POLICY_KEY)
                .orElseGet(PartRevisionWorkflowPolicy::createDefault);
        policy.changeMode(mode);
        return partRevisionWorkflowPolicyRepository.save(policy);
    }
}
