package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void capture_엔티티_입력시_연관과_FK를_동기화한다() {
        Drawing drawing = new Drawing("D-001", "Assembly");
        Part part = Part.create("P-001", "Bolt");
        part.assignDrawing(drawing);

        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob synthesisJob = SynthesisJob.create(mappingRecord, file);

        PartRevision snapshot = PartRevision.capture(part, synthesisJob);

        assertEquals(part, snapshot.getPart());
        assertEquals(part.getId(), snapshot.getPartId());
        assertEquals(drawing, snapshot.getDrawing());
        assertEquals(drawing.getId(), snapshot.getDrawingId());
        assertEquals(synthesisJob, snapshot.getSynthesisJob());
        assertEquals(synthesisJob.getId(), snapshot.getSynthesisJobId());
    }

    @Test
    void capture_파트가_null이면_예외를_던진다() {
        UUID jobId = UUID.randomUUID();

        DomainException ex = assertThrows(DomainException.class, () -> PartRevision.capture(null, jobId));

        assertEquals(PartRevision.CODE_PART_REVISION_PART_REQUIRED, ex.getDomainCode());
    }

    @Test
    void capture_합성작업이_null이면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> PartRevision.capture(part, (SynthesisJob) null));

        assertEquals(PartRevision.CODE_PART_REVISION_SYNTHESIS_JOB_REQUIRED, ex.getDomainCode());
    }
}
