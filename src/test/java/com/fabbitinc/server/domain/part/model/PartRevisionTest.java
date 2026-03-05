package com.fabbitinc.server.domain.part.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartRevisionTest {

    @Test
    void capture_현재_Part_상태를_스냅샷으로_생성한다() {
        Part part = Part.create("P-001", "Bolt");
        part.assignOwner(UUID.randomUUID());
        part.assignOwnerTeam(UUID.randomUUID());
        part.assignDrawing(UUID.randomUUID());
        part.changeCategory("FASTENER");

        UUID jobId = UUID.randomUUID();
        PartRevision snapshot = PartRevision.capture(part, jobId);

        assertEquals(part.getId(), snapshot.getPartId());
        assertEquals(jobId, snapshot.getSynthesisJobId());
        assertEquals(part.getPartNumber(), snapshot.getPartNumber());
        assertEquals(part.getRevision(), snapshot.getRevision());
        assertEquals(part.getCategory(), snapshot.getCategory());
    }
}
