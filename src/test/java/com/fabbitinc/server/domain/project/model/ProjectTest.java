package com.fabbitinc.server.domain.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void create_프로젝트를_초기상태로_생성한다() {
        Project project = Project.create("  신규 프로젝트  ", "  설명  ", UUID.randomUUID());

        assertEquals("신규 프로젝트", project.getName());
        assertEquals("설명", project.getDescription());
        assertFalse(project.isArchived());
        assertTrue(project.getMembers().isEmpty());
        assertTrue(project.getParts().isEmpty());
    }

    @Test
    void create_이름이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Project.create("   ", "설명", UUID.randomUUID()));

        assertEquals(Project.CODE_PROJECT_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void rename_보관된_프로젝트는_수정할_수_없다() {
        UUID actorId = java.util.UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);
        project.archive(actorId);

        DomainException ex = assertThrows(DomainException.class, () -> project.rename("새 이름", actorId));

        assertEquals(Project.CODE_PROJECT_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void archive_이미_보관된_프로젝트면_예외를_던진다() {
        UUID actorId = java.util.UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);
        project.archive(actorId);

        DomainException ex = assertThrows(DomainException.class, () -> project.archive(actorId));

        assertEquals(Project.CODE_PROJECT_ALREADY_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void unarchive_보관상태가_아니면_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> project.unarchive(actorId));

        assertEquals(Project.CODE_PROJECT_NOT_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void unarchive_보관된_프로젝트를_복원한다() {
        UUID actorId = java.util.UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);
        project.archive(actorId);

        project.unarchive(actorId);

        assertFalse(project.isArchived());
    }

    @Test
    void changeDescription_blank면_null로_정규화한다() {
        UUID actorId = java.util.UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);

        project.changeDescription("   ", actorId);

        assertNull(project.getDescription());
    }

    @Test
    void addMember_보관된_프로젝트는_수정할_수_없다() {
        UUID actorId = java.util.UUID.randomUUID();
        Project project = Project.create("프로젝트", "설명", actorId);
        project.archive(actorId);

        DomainException ex = assertThrows(DomainException.class, () ->
                project.addMember(java.util.UUID.randomUUID(), ProjectRole.MEMBER)
        );

        assertEquals(Project.CODE_PROJECT_ARCHIVED, ex.getDomainCode());
    }
}
