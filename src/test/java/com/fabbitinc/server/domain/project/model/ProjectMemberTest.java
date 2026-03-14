package com.fabbitinc.server.domain.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectMemberTest {

    @Test
    void addMember_사용자_ID로_멤버를_추가한다() {
        Project project = Project.create("프로젝트", "설명", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        ProjectMember member = project.addMember(userId, ProjectRole.ADMIN);

        assertEquals(project, member.getProject());
        assertEquals(project.getId(), member.getProjectId());
        assertEquals(userId, member.getUserId());
        assertEquals(ProjectRole.ADMIN, member.getRole());
        assertEquals(1, project.getMembers().size());
    }

    @Test
    void addMember_역할이_null이면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        DomainException ex = assertThrows(
                DomainException.class,
                () -> project.addMember(userId, null)
        );

        assertEquals(ProjectMember.CODE_PROJECT_MEMBER_ROLE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void addMember_사용자가_null이면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명", UUID.randomUUID());

        DomainException ex = assertThrows(
                DomainException.class,
                () -> project.addMember(null, ProjectRole.MEMBER)
        );

        assertEquals(ProjectMember.CODE_PROJECT_MEMBER_USER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void changeMemberRole_프로젝트루트에서_멤버역할을_변경한다() {
        Project project = Project.create("프로젝트", "설명", UUID.randomUUID());
        ProjectMember member = project.addMember(UUID.randomUUID(), ProjectRole.MEMBER);

        project.changeMemberRole(member, ProjectRole.ADMIN);

        assertEquals(ProjectRole.ADMIN, member.getRole());
    }

    @Test
    void changeMemberRole_다른프로젝트_멤버면_예외를_던진다() {
        Project project = Project.create("프로젝트", "설명", UUID.randomUUID());
        Project otherProject = Project.create("다른 프로젝트", "설명", UUID.randomUUID());
        ProjectMember foreignMember = otherProject.addMember(UUID.randomUUID(), ProjectRole.MEMBER);

        DomainException ex = assertThrows(
                DomainException.class,
                () -> project.changeMemberRole(foreignMember, ProjectRole.ADMIN)
        );

        assertEquals(Project.CODE_PROJECT_MEMBER_MISMATCH, ex.getDomainCode());
    }
}
