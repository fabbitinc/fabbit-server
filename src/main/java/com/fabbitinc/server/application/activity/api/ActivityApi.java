package com.fabbitinc.server.application.activity.api;

import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityApi {

    private final ActivityRepository activityRepository;

    public List<Activity> listTargetHistories(ActivityTargetType targetType, UUID targetId) {
        return activityRepository.findByTargetTypeAndTargetIdOrderByIdDesc(targetType, targetId);
    }
}
