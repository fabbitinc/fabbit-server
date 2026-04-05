package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeStepRepository extends JpaRepository<EngineeringChangeStep, UUID> {

    List<EngineeringChangeStep> findByEngineeringChangeIdOrderByCreatedAtAsc(UUID engineeringChangeId);

    List<EngineeringChangeStep> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    List<EngineeringChangeStep> findByStepStageIdOrderByCreatedAtAsc(UUID stepStageId);

    List<EngineeringChangeStep> findByStepStageIdAndStatusOrderByCreatedAtAsc(
            UUID stepStageId,
            EngineeringChangeStepStatus status
    );

    List<EngineeringChangeStep> findByEngineeringChangeIdAndStatusOrderByCreatedAtAsc(
            UUID engineeringChangeId,
            EngineeringChangeStepStatus status
    );

    void deleteByEngineeringChangeId(UUID engineeringChangeId);

    void deleteByStepStageId(UUID stepStageId);
}
