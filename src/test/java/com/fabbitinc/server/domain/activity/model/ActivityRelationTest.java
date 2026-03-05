package com.fabbitinc.server.domain.activity.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityRelationTest {

    @Test
    void activity_엔티티_입력시_actor_FK와_연관을_동기화한다() {
        User actor = new User("actor@example.com", "hashed", "Actor");
        UUID targetId = UUID.randomUUID();

        Activity activity = Activity.create(
                ActivityTargetType.ISSUE,
                targetId,
                "issue:created",
                actor,
                "{}"
        );

        assertEquals(actor, activity.getActor());
        assertEquals(actor.getId(), activity.getActorId());
        assertEquals(targetId, activity.getTargetId());
    }

    @Test
    void activity_행위자가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Activity.create(
                ActivityTargetType.ISSUE,
                UUID.randomUUID(),
                "issue:created",
                (User) null,
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
}
