package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartDefaultOwnerTest {

    @Test
    void create_ID_입력시_기본담당자를_설정한다() {
        UUID ownerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        PartDefaultOwner row = PartDefaultOwner.create("FASTENER", ownerId, teamId);

        assertEquals(ownerId, row.getDefaultOwnerId());
        assertEquals(teamId, row.getDefaultOwnerTeamId());
    }

    @Test
    void update_ID_입력시_기본담당자를_변경한다() {
        PartDefaultOwner row = PartDefaultOwner.create("FASTENER", UUID.randomUUID(), UUID.randomUUID());
        UUID newOwnerId = UUID.randomUUID();
        UUID newTeamId = UUID.randomUUID();

        row.update(newOwnerId, newTeamId);

        assertEquals(newOwnerId, row.getDefaultOwnerId());
        assertEquals(newTeamId, row.getDefaultOwnerTeamId());
    }

    @Test
    void create_카테고리는_trim_정규화한다() {
        PartDefaultOwner row = PartDefaultOwner.create("  FASTENER  ", UUID.randomUUID(), null);

        assertEquals("FASTENER", row.getCategory());
    }

    @Test
    void create_카테고리_길이초과면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                PartDefaultOwner.create("a".repeat(101), (UUID) null, null)
        );

        assertEquals(PartDefaultOwner.CODE_PART_DEFAULT_OWNER_CATEGORY_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void create_기본담당자와_기본담당팀이_둘다없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                PartDefaultOwner.create("FASTENER", null, null)
        );

        assertEquals(PartDefaultOwner.CODE_PART_DEFAULT_OWNER_TARGET_REQUIRED, ex.getDomainCode());
    }
}
