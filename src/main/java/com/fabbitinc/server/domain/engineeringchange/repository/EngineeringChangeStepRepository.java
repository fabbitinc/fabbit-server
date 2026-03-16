package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeStepRepository extends JpaRepository<EngineeringChangeStep, UUID> {

    List<EngineeringChangeStep> findByEngineeringChangeIdOrderBySequenceAscCreatedAtAsc(UUID engineeringChangeId);

    List<EngineeringChangeStep> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    List<EngineeringChangeStep> findByEngineeringChangeIdAndStepTypeOrderBySequenceAscCreatedAtAsc(
            UUID engineeringChangeId,
            EngineeringChangeStepType stepType
    );

    List<EngineeringChangeStep> findByEngineeringChangeIdAndStepTypeAndStatusOrderBySequenceAscCreatedAtAsc(
            UUID engineeringChangeId,
            EngineeringChangeStepType stepType,
            EngineeringChangeStepStatus status
    );

    void deleteByEngineeringChangeId(UUID engineeringChangeId);

    void deleteByEngineeringChangeIdAndStepType(UUID engineeringChangeId, EngineeringChangeStepType stepType);
}
