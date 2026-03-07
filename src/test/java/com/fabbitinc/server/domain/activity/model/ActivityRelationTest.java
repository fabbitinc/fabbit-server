package com.fabbitinc.server.domain.activity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityRelationTest {

    @Test
    void activity_create_입력값을_보관한다() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Activity activity = Activity.create(
                ActivityTargetType.ISSUE,
                targetId,
                "issue:created",
                actorId,
                "  {}  "
        );

        assertEquals(actorId, activity.getActorId());
        assertEquals(targetId, activity.getTargetId());
        assertEquals("{}", activity.getDetail());
    }

    @Test
    void activity_행위자가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Activity.create(
                ActivityTargetType.ISSUE,
                UUID.randomUUID(),
                "issue:created",
                null,
                "{}"
        ));

        assertEquals(Activity.CODE_ACTIVITY_ACTOR_REQUIRED, ex.getDomainCode());
    }

    @Test
    void activity_action이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Activity.create(
                ActivityTargetType.ISSUE,
                UUID.randomUUID(),
                "   ",
                UUID.randomUUID(),
                "{}"
        ));

        assertEquals(Activity.CODE_ACTIVITY_ACTION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void activity_targetType이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Activity.create(
                null,
                UUID.randomUUID(),
                "issue:created",
                UUID.randomUUID(),
                "{}"
        ));

        assertEquals(Activity.CODE_ACTIVITY_TARGET_TYPE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void activity_detail이_비어있으면_null로_정규화한다() {
        Activity activity = Activity.create(
                ActivityTargetType.ISSUE,
                UUID.randomUUID(),
                "issue:created",
                UUID.randomUUID(),
                "   "
        );

        assertEquals(null, activity.getDetail());
    }
}
