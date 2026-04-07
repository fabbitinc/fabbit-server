package com.fabbitinc.server.application.engineeringchange.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EngineeringChangeRevisionLockApi {

    private final EngineeringChangeAffectedItemRepository affectedItemRepository;
    private final EngineeringChangeRepository engineeringChangeRepository;

    public void assertRevisionEditable(UUID revisionId) {
        List<EngineeringChangeAffectedItem> links = affectedItemRepository.findByTargetIdAndItemTypeOrderByCreatedAtAsc(
                revisionId,
                EngineeringChangeAffectedItemType.REVISION_RELEASE
        );
        if (links.isEmpty()) {
            return;
        }

        engineeringChangeRepository.findAllById(
                        links.stream()
                                .map(EngineeringChangeAffectedItem::getEngineeringChangeId)
                                .distinct()
                                .toList()
                ).stream()
                .filter(ec -> ec.getState() == EngineeringChangeState.REVIEW_PENDING
                        || ec.getState() == EngineeringChangeState.APPROVAL_PENDING
                        || ec.getState() == EngineeringChangeState.RELEASE_PENDING)
                .findFirst()
                .ifPresent(ec -> {
                    throw new AppException(
                            ErrorCode.CONFLICT,
                            "진행 중인 EC에 연결된 변경 대상 draft revision은 수정할 수 없습니다: EC-%d".formatted(ec.getNumber())
                    );
                });
    }
}
