package com.fabbitinc.server.domain.label.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabelRelationTest {

    @Test
    void label_엔티티_입력시_createdBy_updatedBy_FK와_연관을_동기화한다() {
        User actor = new User("actor@example.com", "hashed", "Actor");

        Label label = Label.create("bug", null, "#ff0000", actor);

        assertEquals(actor.getId(), label.getCreatedBy());
        assertEquals(actor, label.getCreatedByUser());
        assertEquals(actor.getId(), label.getUpdatedBy());
        assertEquals(actor, label.getUpdatedByUser());
    }

    @Test
    void label_색상변경_엔티티입력시_updatedBy_FK와_연관을_동기화한다() {
        UUID actorId = UUID.randomUUID();
        Label label = Label.create("bug", null, "#ff0000", actorId);
        User actor = new User("actor@example.com", "hashed", "Actor");

        label.changeColor("#00ff00", actor);

        assertEquals(actor.getId(), label.getUpdatedBy());
        assertEquals(actor, label.getUpdatedByUser());
    }

    @Test
    void label_이름은_trim_정규화한다() {
        Label label = Label.create("  bug  ", "  설명  ", "#ff0000", UUID.randomUUID());

        assertEquals("bug", label.getName());
        assertEquals("설명", label.getDescription());
    }

    @Test
    void label_색상형식이_잘못되면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Label.create("bug", null, "red", UUID.randomUUID())
        );

        assertEquals(Label.CODE_LABEL_COLOR_INVALID, ex.getDomainCode());
    }

    @Test
    void label_수행자가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Label.create("bug", null, "#ff0000", (UUID) null)
        );

        assertEquals(Label.CODE_LABEL_ACTOR_REQUIRED, ex.getDomainCode());
    }
}
