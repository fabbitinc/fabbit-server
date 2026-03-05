package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BomLinkTest {

    @Test
    void connect_수량이_0이하면_예외를_던진다() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        DomainException ex = assertThrows(
                DomainException.class,
                () -> BomLink.connect(parentId, childId, 0, "{}")
        );

        assertEquals(BomLink.CODE_BOM_INVALID_QUANTITY, ex.getDomainCode());
    }
}
