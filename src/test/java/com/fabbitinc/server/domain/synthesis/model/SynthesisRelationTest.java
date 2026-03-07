package com.fabbitinc.server.domain.synthesis.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SynthesisRelationTest {

    @Test
    void synthesisBatch_create_입력값을_보관한다() {
        UUID projectId = UUID.randomUUID();
        UUID mappingId = UUID.randomUUID();

        SynthesisBatch batch = SynthesisBatch.create(projectId, mappingId, 10, "  []  ");

        assertEquals(projectId, batch.getProjectId());
        assertEquals(mappingId, batch.getMappingId());
        assertEquals(10, batch.getRequestedCount());
        assertEquals(0, batch.getAcceptedCount());
        assertEquals("[]", batch.getFailedUploads());
        assertTrue(batch.getJobs().isEmpty());
    }

    @Test
    void synthesisBatch_요청건수가_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                -1,
                "[]"
        ));

        assertEquals(SynthesisBatch.CODE_SYNTHESIS_BATCH_REQUESTED_COUNT_INVALID, ex.getDomainCode());
    }

    @Test
    void synthesisBatch_addJob은_acceptedCount를_증가시키고_job을_연결한다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                "[]"
        );
        UUID fileId = UUID.randomUUID();

        SynthesisJob job = batch.addJob(fileId);

        assertEquals(batch.getId(), job.getBatchId());
        assertEquals(batch.getMappingId(), job.getMappingId());
        assertEquals(fileId, job.getFileId());
        assertEquals(1, batch.getAcceptedCount());
        assertEquals(1, batch.getJobs().size());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }

    @Test
    void synthesisBatch_addJob은_요청건수를_초과하면_예외를_던진다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "[]"
        );
        batch.addJob(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> batch.addJob(UUID.randomUUID()));

        assertEquals(SynthesisBatch.CODE_SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID, ex.getDomainCode());
    }

    @Test
    void synthesisJob_정상_상태전이를_수행한다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "[]"
        );
        SynthesisJob job = batch.addJob(UUID.randomUUID());

        job.start(3);
        job.complete(3, 2, 1, "[\"warn\"]");

        assertEquals(SynthesisJobStatus.COMPLETED, job.getStatus());
        assertEquals(3, job.getTotalRows());
        assertEquals(3, job.getProcessedRows());
        assertEquals(2, job.getNodesCreated());
        assertEquals(1, job.getRelationshipsCreated());
        assertEquals("[\"warn\"]", job.getErrors());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void synthesisJob_complete는_PROCESSING에서만_허용한다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "[]"
        );
        SynthesisJob job = batch.addJob(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> job.complete(0, 0, 0, "[]"));

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_INVALID_STATE, ex.getDomainCode());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }

    @Test
    void synthesisJob_complete는_처리행수가_전체행수를_초과하면_예외를_던진다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "[]"
        );
        SynthesisJob job = batch.addJob(UUID.randomUUID());
        job.start(1);

        DomainException ex = assertThrows(DomainException.class, () -> job.complete(2, 0, 0, "[]"));

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_PROGRESS_INVALID, ex.getDomainCode());
    }

    @Test
    void synthesisJob_fail은_PROCESSING에서만_허용한다() {
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "[]"
        );
        SynthesisJob job = batch.addJob(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> job.fail("[\"error\"]"));

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_INVALID_STATE, ex.getDomainCode());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }
}
