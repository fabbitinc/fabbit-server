package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectPartTest {

    @Test
    void link_엔티티_입력시_연관과_ID를_동기화한다() {
        Project project = Project.create("프로젝트", "설명");
        Part part = Part.create("P-001", "Bolt");

        ProjectPart link = ProjectPart.link(project, part);

        assertEquals(project, link.getProject());
        assertEquals(part, link.getPart());
        assertEquals(project.getId(), link.getProjectId());
        assertEquals(part.getId(), link.getPartId());
    }

    @Test
    void link_partId가_null이면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> ProjectPart.link(project.getId(), (UUID) null)
        );

        assertEquals(ProjectPart.CODE_PROJECT_PART_PART_REQUIRED, ex.getDomainCode());
    }
}
