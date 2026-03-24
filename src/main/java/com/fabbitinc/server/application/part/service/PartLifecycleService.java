package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartLifecycleService {

    private final PartRepository partRepository;

    public Part changeLifecycleState(UUID partId, PartLifecycleState targetState, UUID actorId) {
        try {
            Part part = getRequiredPartForUpdate(partId);
            part.changeLifecycleState(targetState);
            return part;
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public Part changeLifecycleStateFromEngineeringChange(
            UUID partId, PartLifecycleState targetState, UUID actorId, UUID engineeringChangeId
    ) {
        try {
            Part part = getRequiredPartForUpdate(partId);
            part.changeLifecycleState(targetState);
            return part;
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public Part revertLifecycleState(UUID partId, PartLifecycleState previousState) {
        Part part = getRequiredPartForUpdate(partId);
        part.forceLifecycleState(previousState);
        return part;
    }

    private Part getRequiredPartForUpdate(UUID partId) {
        return partRepository.findByIdForUpdate(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partId)
                ));
    }
}
