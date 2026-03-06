package com.fabbitinc.server.domain.team.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRelationTest {

    @Test
    void team_기본_멤버_컬렉션은_비어있다() {
        Team team = new Team("Core Team", null, UUID.randomUUID());

        assertTrue(team.getMembers().isEmpty());
    }

    @Test
    void team_엔티티_입력시_createdBy_FK와_연관을_동기화한다() {
        User creator = new User("creator@example.com", "hashed", "Creator");

        Team team = Team.create("  Core Team  ", "  설명  ", creator);

        assertEquals("Core Team", team.getName());
        assertEquals("설명", team.getDescription());
        assertEquals(creator.getId(), team.getCreatedBy());
        assertEquals(creator, team.getCreator());
    }

    @Test
    void team_이름이_blank면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Team.create("   ", null, UUID.randomUUID())
        );

        assertEquals(Team.CODE_TEAM_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void team_changeDescription_blank면_null로_정규화한다() {
        Team team = Team.create("Core Team", "설명", UUID.randomUUID());

        team.changeDescription("   ");

        assertNull(team.getDescription());
    }

    @Test
    void teamMember_엔티티_입력시_FK와_연관을_동기화한다() {
        Team team = new Team("Core Team", null, UUID.randomUUID());
        User user = new User("team-member@example.com", "hashed", "Team Member");

        TeamMember member = TeamMember.assign(team, user);

        assertEquals(team, member.getTeam());
        assertEquals(user, member.getUser());
        assertEquals(team.getId(), member.getTeamId());
        assertEquals(user.getId(), member.getUserId());
    }

    @Test
    void teamMember_user가_null이면_예외를_던진다() {
        Team team = new Team("Core Team", null, UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> TeamMember.assign(team, (User) null));

        assertEquals(TeamMember.CODE_TEAM_MEMBER_USER_REQUIRED, ex.getDomainCode());
    }
}
