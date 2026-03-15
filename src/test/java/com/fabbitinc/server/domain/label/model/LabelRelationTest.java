package com.fabbitinc.server.domain.label.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LabelRelationTest {

    @Test
    void label_생성시_createdBy와_updatedBy를_설정한다() {
        UUID actorId = UUID.randomUUID();
        Label label = Label.create("bug", null, "#ff0000", actorId);

        assertEquals(actorId, label.getCreatedBy());
        assertEquals(actorId, label.getUpdatedBy());
    }

    @Test
    void system_default_label_생성시_createdBy와_updatedBy는_null이다() {
        Label label = Label.createSystemDefault("기본", "설명", "#ff0000");

        assertNull(label.getCreatedBy());
        assertNull(label.getUpdatedBy());
    }

    @Test
    void label_색상변경시_updatedBy를_갱신한다() {
        UUID createdActorId = UUID.randomUUID();
        UUID updatedActorId = UUID.randomUUID();
        Label label = Label.create("bug", null, "#ff0000", createdActorId);

        label.changeColor("#00ff00", updatedActorId);

        assertEquals("#00ff00", label.getColor());
        assertEquals(updatedActorId, label.getUpdatedBy());
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

    @Test
    void label_changeColor_수행자가_null이면_색상과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Label label = Label.create("bug", null, "#ff0000", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> label.changeColor("#00ff00", (UUID) null));

        assertEquals(Label.CODE_LABEL_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals("#ff0000", label.getColor());
        assertEquals(actorId, label.getUpdatedBy());
    }

    @Test
    void label_changeDescription_수행자가_null이면_설명과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Label label = Label.create("bug", "원본", "#ff0000", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> label.changeDescription("변경", (UUID) null));

        assertEquals(Label.CODE_LABEL_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals("원본", label.getDescription());
        assertEquals(actorId, label.getUpdatedBy());
    }

    @Test
    void label_removeDescription_수행자가_null이면_설명과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Label label = Label.create("bug", "원본", "#ff0000", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> label.removeDescription((UUID) null));

        assertEquals(Label.CODE_LABEL_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals("원본", label.getDescription());
        assertEquals(actorId, label.getUpdatedBy());
    }
}
