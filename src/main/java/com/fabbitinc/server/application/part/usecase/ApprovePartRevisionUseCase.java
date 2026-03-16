package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.usecase.command.ApprovePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.ApprovePartRevisionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ApprovePartRevisionUseCase {

    private final CurrentAuthProvider currentAuthProvider;

    public ApprovePartRevisionResult execute(ApprovePartRevisionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        throw new AppException(
                ErrorCode.PART_WORKFLOW_POLICY_FORBIDDEN,
                "직접 반영 모드에서는 승인 단계를 사용하지 않습니다. EngineeringChange 승인 흐름을 사용해 주세요"
        );
    }
}
