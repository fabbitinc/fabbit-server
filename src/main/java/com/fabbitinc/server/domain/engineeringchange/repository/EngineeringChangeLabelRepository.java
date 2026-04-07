package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeLabel;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeLabelRepository extends JpaRepository<EngineeringChangeLabel, UUID> {

    List<EngineeringChangeLabel> findByEngineeringChangeId(UUID engineeringChangeId);

    List<EngineeringChangeLabel> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    int deleteByEngineeringChangeIdAndLabelIdIn(UUID engineeringChangeId, Collection<UUID> labelIds);
}
