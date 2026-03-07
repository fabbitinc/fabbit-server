package com.fabbitinc.server.domain.activity.repository;

import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByTargetTypeAndTargetIdOrderByIdDesc(ActivityTargetType targetType, UUID targetId);

    List<Activity> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ActivityTargetType targetType, UUID targetId);
}
