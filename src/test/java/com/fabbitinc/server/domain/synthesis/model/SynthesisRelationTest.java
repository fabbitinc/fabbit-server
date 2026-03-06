package com.fabbitinc.server.domain.synthesis.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import com.fabbitinc.server.domain.project.model.Project;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynthesisRelationTest {

    @Test
    void synthesisBatch_엔티티_입력시_FK와_연관을_동기화한다() {
        Project project = Project.create("프로젝트", "설명");
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);

        SynthesisBatch batch = SynthesisBatch.create(project, mappingRecord, 10, 8, "  []  ");

        assertEquals(project, batch.getProject());
        assertEquals(mappingRecord, batch.getMappingRecord());
        assertEquals(project.getId(), batch.getProjectId());
        assertEquals(mappingRecord.getId(), batch.getMappingId());
        assertEquals("[]", batch.getFailedUploads());
        assertTrue(batch.getJobs().isEmpty());
    }

    @Test
    void synthesisBatch_요청건수가_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                -1,
                0,
                "[]"
        ));

        assertEquals(SynthesisBatch.CODE_SYNTHESIS_BATCH_REQUESTED_COUNT_INVALID, ex.getDomainCode());
    }

    @Test
    void synthesisBatch_수락건수가_요청건수보다_크면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> SynthesisBatch.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                2,
                "[]"
        ));

        assertEquals(SynthesisBatch.CODE_SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID, ex.getDomainCode());
    }

    @Test
    void synthesisJob_엔티티_입력시_FK와_연관을_동기화한다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);

        SynthesisJob job = SynthesisJob.create(mappingRecord, file);

        assertEquals(mappingRecord, job.getMappingRecord());
        assertEquals(file, job.getFile());
        assertEquals(mappingRecord.getId(), job.getMappingId());
        assertEquals(file.getId(), job.getFileId());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }

    @Test
    void assignBatch_엔티티_입력시_batchId와_연관을_동기화한다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob job = SynthesisJob.create(mappingRecord, file);
        SynthesisBatch batch = SynthesisBatch.create(
                UUID.randomUUID(),
                mappingRecord.getId(),
                1,
                1,
                "[]"
        );

        job.assignBatch(batch);

        assertEquals(batch, job.getBatch());
        assertEquals(batch.getId(), job.getBatchId());
    }

    @Test
    void assignBatch_null이면_예외를_던진다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob job = SynthesisJob.create(mappingRecord, file);

        DomainException ex = assertThrows(DomainException.class, () -> job.assignBatch((SynthesisBatch) null));

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_BATCH_REQUIRED, ex.getDomainCode());
    }

    @Test
    void synthesisJob_정상_상태전이를_수행한다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob job = SynthesisJob.create(mappingRecord, file);

        job.markProcessing();
        job.markCompleted();

        assertEquals(SynthesisJobStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void synthesisJob_markCompleted는_PROCESSING에서만_허용한다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob job = SynthesisJob.create(mappingRecord, file);

        DomainException ex = assertThrows(DomainException.class, job::markCompleted);

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_INVALID_STATE, ex.getDomainCode());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }

    @Test
    void synthesisJob_markFailed는_PROCESSING에서만_허용한다() {
        MappingRecord mappingRecord = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 128L);
        SynthesisJob job = SynthesisJob.create(mappingRecord, file);

        DomainException ex = assertThrows(DomainException.class, () -> job.markFailed("[\"error\"]"));

        assertEquals(SynthesisJob.CODE_SYNTHESIS_JOB_INVALID_STATE, ex.getDomainCode());
        assertEquals(SynthesisJobStatus.PENDING, job.getStatus());
    }
}
