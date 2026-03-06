package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {

    @Test
    void create_프로젝트를_초기상태로_생성한다() {
        Project project = Project.create("  신규 프로젝트  ", "  설명  ");

        assertEquals("신규 프로젝트", project.getName());
        assertEquals("설명", project.getDescription());
        assertFalse(project.isArchived());
        assertTrue(project.getMembers().isEmpty());
        assertTrue(project.getParts().isEmpty());
    }

    @Test
    void create_이름이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Project.create("   ", "설명"));

        assertEquals(Project.CODE_PROJECT_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void rename_보관된_프로젝트는_수정할_수_없다() {
        Project project = Project.create("프로젝트", "설명");
        project.archive();

        DomainException ex = assertThrows(DomainException.class, () -> project.rename("새 이름"));

        assertEquals(Project.CODE_PROJECT_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void archive_이미_보관된_프로젝트면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명");
        project.archive();

        DomainException ex = assertThrows(DomainException.class, project::archive);

        assertEquals(Project.CODE_PROJECT_ALREADY_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void unarchive_보관상태가_아니면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명");

        DomainException ex = assertThrows(DomainException.class, project::unarchive);

        assertEquals(Project.CODE_PROJECT_NOT_ARCHIVED, ex.getDomainCode());
    }

    @Test
    void unarchive_보관된_프로젝트를_복원한다() {
        Project project = Project.create("프로젝트", "설명");
        project.archive();

        project.unarchive();

        assertFalse(project.isArchived());
    }

    @Test
    void changeDescription_blank면_null로_정규화한다() {
        Project project = Project.create("프로젝트", "설명");

        project.changeDescription("   ");

        assertNull(project.getDescription());
    }
}
