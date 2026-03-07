package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartRevisionTest {

    @Test
    void capture_현재_Part_상태를_스냅샷으로_생성한다() {
        Part part = Part.create("P-001", "Bolt");
        part.assignOwner(UUID.randomUUID());
        part.assignOwnerTeam(UUID.randomUUID());
        UUID drawingId = UUID.randomUUID();
        part.assignDrawing(drawingId);
        part.changeCategory("FASTENER");

        UUID jobId = UUID.randomUUID();
        PartRevision snapshot = PartRevision.capture(part, jobId);

        assertEquals(part.getId(), snapshot.getPartId());
        assertEquals(jobId, snapshot.getSynthesisJobId());
        assertEquals(part.getPartNumber(), snapshot.getPartNumber());
        assertEquals(part.getRevision(), snapshot.getRevision());
        assertEquals(part.getCategory(), snapshot.getCategory());
        assertEquals(drawingId, snapshot.getDrawingId());
    }

    @Test
    void capture_도면과_합성작업_ID를_스냅샷에_보관한다() {
        Part part = Part.create("P-001", "Bolt");
        UUID drawingId = UUID.randomUUID();
        UUID synthesisJobId = UUID.randomUUID();
        part.assignDrawing(drawingId);

        PartRevision snapshot = PartRevision.capture(part, synthesisJobId);

        assertEquals(part.getId(), snapshot.getPartId());
        assertEquals(drawingId, snapshot.getDrawingId());
        assertEquals(synthesisJobId, snapshot.getSynthesisJobId());
    }

    @Test
    void capture_파트가_null이면_예외를_던진다() {
        UUID jobId = UUID.randomUUID();

        DomainException ex = assertThrows(DomainException.class, () -> PartRevision.capture(null, jobId));

        assertEquals(PartRevision.CODE_PART_REVISION_PART_REQUIRED, ex.getDomainCode());
    }
}
