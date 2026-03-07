package com.fabbitinc.server.domain.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectPartTest {

    @Test
    void linkPart_부품_ID로_링크를_추가한다() {
        Project project = Project.create("프로젝트", "설명");
        UUID partId = UUID.randomUUID();

        ProjectPart link = project.linkPart(partId);

        assertEquals(project, link.getProject());
        assertEquals(project.getId(), link.getProjectId());
        assertEquals(partId, link.getPartId());
        assertEquals(1, project.getParts().size());
    }

    @Test
    void link_partId가_null이면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> project.linkPart(null)
        );

        assertEquals(ProjectPart.CODE_PROJECT_PART_PART_REQUIRED, ex.getDomainCode());
    }
}
