package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeAffectedItemRepository extends JpaRepository<EngineeringChangeAffectedItem, UUID> {

    List<EngineeringChangeAffectedItem> findByEngineeringChangeIdOrderByCreatedAtAsc(UUID engineeringChangeId);

    List<EngineeringChangeAffectedItem> findByEngineeringChangeIdAndItemTypeOrderByCreatedAtAsc(
            UUID engineeringChangeId, EngineeringChangeAffectedItemType itemType);

    List<EngineeringChangeAffectedItem> findByTargetIdAndItemTypeOrderByCreatedAtAsc(
            UUID targetId,
            EngineeringChangeAffectedItemType itemType
    );

    boolean existsByTargetIdAndItemType(UUID targetId, EngineeringChangeAffectedItemType itemType);

    void deleteByEngineeringChangeId(UUID engineeringChangeId);
}
