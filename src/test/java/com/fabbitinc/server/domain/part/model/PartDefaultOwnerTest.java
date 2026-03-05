package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PartDefaultOwnerTest {

    @Test
    void create_엔티티_입력시_FK와_연관을_동기화한다() {
        User owner = new User("owner@example.com", "hashed", "Owner");
        Team team = new Team("기본팀", null, UUID.randomUUID());

        PartDefaultOwner row = PartDefaultOwner.create("FASTENER", owner, team);

        assertEquals(owner, row.getDefaultOwner());
        assertEquals(owner.getId(), row.getDefaultOwnerId());
        assertEquals(team, row.getDefaultOwnerTeam());
        assertEquals(team.getId(), row.getDefaultOwnerTeamId());
    }

    @Test
    void update_ID_입력시_불일치_연관을_초기화한다() {
        User owner = new User("owner@example.com", "hashed", "Owner");
        Team team = new Team("기본팀", null, UUID.randomUUID());
        PartDefaultOwner row = PartDefaultOwner.create("FASTENER", owner, team);

        row.update(UUID.randomUUID(), UUID.randomUUID());

        assertNull(row.getDefaultOwner());
        assertNull(row.getDefaultOwnerTeam());
    }

    @Test
    void update_엔티티_입력시_FK와_연관을_동기화한다() {
        PartDefaultOwner row = PartDefaultOwner.create("FASTENER", (UUID) null, null);
        User owner = new User("owner@example.com", "hashed", "Owner");
        Team team = new Team("기본팀", null, UUID.randomUUID());

        row.update(owner, team);

        assertEquals(owner, row.getDefaultOwner());
        assertEquals(owner.getId(), row.getDefaultOwnerId());
        assertEquals(team, row.getDefaultOwnerTeam());
        assertEquals(team.getId(), row.getDefaultOwnerTeamId());
    }

    @Test
    void create_카테고리는_trim_정규화한다() {
        PartDefaultOwner row = PartDefaultOwner.create("  FASTENER  ", (UUID) null, null);

        assertEquals("FASTENER", row.getCategory());
    }
}
