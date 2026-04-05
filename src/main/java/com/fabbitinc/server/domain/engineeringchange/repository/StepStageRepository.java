package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepStageRepository extends JpaRepository<StepStage, UUID> {

    List<StepStage> findByEngineeringChangeIdOrderBySequenceAsc(UUID engineeringChangeId);

    List<StepStage> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    void deleteByEngineeringChangeId(UUID engineeringChangeId);
}
