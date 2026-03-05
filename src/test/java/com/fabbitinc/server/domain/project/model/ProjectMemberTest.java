package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectMemberTest {

    @Test
    void assign_엔티티_입력시_연관과_ID를_동기화한다() {
        Project project = Project.create("프로젝트", "설명");
        User user = new User("member@example.com", "hashed", "Member");

        ProjectMember member = ProjectMember.assign(project, user, ProjectRole.ADMIN);

        assertEquals(project, member.getProject());
        assertEquals(user, member.getUser());
        assertEquals(project.getId(), member.getProjectId());
        assertEquals(user.getId(), member.getUserId());
        assertEquals(ProjectRole.ADMIN, member.getRole());
    }

    @Test
    void assign_역할이_null이면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명");
        User user = new User("member@example.com", "hashed", "Member");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> ProjectMember.assign(project, user, null)
        );

        assertEquals(ProjectMember.CODE_PROJECT_MEMBER_ROLE_REQUIRED, ex.getDomainCode());
    }
}
