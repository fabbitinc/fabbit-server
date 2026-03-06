package com.fabbitinc.server.domain.team.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRelationTest {

    @Test
    void team_기본_멤버_컬렉션은_비어있다() {
        Team team = Team.create("Core Team", null, UUID.randomUUID());

        assertTrue(team.getMembers().isEmpty());
    }

    @Test
    void team_생성시_name과_description을_정규화한다() {
        UUID creatorId = UUID.randomUUID();
        Team team = Team.create("  Core Team  ", "  설명  ", creatorId);

        assertEquals("Core Team", team.getName());
        assertEquals("설명", team.getDescription());
        assertEquals(creatorId, team.getCreatedBy());
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
    void team_addMember_사용자_ID로_멤버를_추가한다() {
        Team team = Team.create("Core Team", null, UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        TeamMember member = team.addMember(userId);

        assertEquals(team, member.getTeam());
        assertEquals(team.getId(), member.getTeamId());
        assertEquals(userId, member.getUserId());
        assertEquals(1, team.getMembers().size());
    }

    @Test
    void team_addMember_userId가_null이면_예외를_던진다() {
        Team team = Team.create("Core Team", null, UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> team.addMember(null));

        assertEquals(TeamMember.CODE_TEAM_MEMBER_USER_REQUIRED, ex.getDomainCode());
    }
}
