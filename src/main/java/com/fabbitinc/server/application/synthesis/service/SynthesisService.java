package com.fabbitinc.server.application.synthesis.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesis.service.input.StartSynthesisInput;
import com.fabbitinc.server.application.synthesis.service.input.SynthesisUploadInput;
import com.fabbitinc.server.application.synthesis.service.output.SynthesisBatchFailureOutput;
import com.fabbitinc.server.application.synthesis.service.output.SynthesisBatchStartOutput;
import com.fabbitinc.server.application.synthesis.service.output.SynthesisJobOutput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisBatchRepository;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SynthesisService {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final FileRepository fileRepository;
    private final SynthesisBatchRepository synthesisBatchRepository;
    private final SynthesisJobRepository synthesisJobRepository;
    private final SynthesisAsyncExecutionService synthesisAsyncExecutionService;
    private final ObjectMapper objectMapper;

    public SynthesisBatchStartOutput startSynthesis(StartSynthesisInput input) {
        MappingRecord record = mappingRecordRepository.findByIdAndActiveTrue(input.mappingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));

        MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑 리비전을 찾을 수 없습니다"));

        List<SynthesisBatchFailureOutput> failed = new ArrayList<>();
        List<AcceptedUpload> acceptedUploads = new ArrayList<>();

        for (SynthesisUploadInput item : input.uploads()) {
            validateRootContext(record.getScope(), item.rootContext());

            File file = fileRepository.findByIdAndDeletedAtIsNull(item.fileId()).orElse(null);
            if (file == null) {
                failed.add(new SynthesisBatchFailureOutput(item.fileId(), "파일을 찾을 수 없습니다"));
                continue;
            }
            if (file.getStatus() != FileStatus.UPLOADED) {
                failed.add(new SynthesisBatchFailureOutput(item.fileId(), "업로드가 완료되지 않은 파일입니다"));
                continue;
            }
            if (input.projectId() != null && !isProjectOwnedFile(file, input.projectId())) {
                failed.add(new SynthesisBatchFailureOutput(item.fileId(), "해당 프로젝트에 속하지 않은 파일입니다"));
                continue;
            }

            Map<String, String> rootContext = item.rootContext() == null ? Map.of() : item.rootContext();
            acceptedUploads.add(new AcceptedUpload(file.getId(), rootContext));
        }

        SynthesisBatch batch = SynthesisBatch.create(
                input.projectId(),
                record.getId(),
                input.uploads().size(),
                serializeFailures(failed)
        );
        List<AcceptedSynthesisJob> acceptedJobs = acceptedUploads.stream()
                .map(acceptedUpload -> new AcceptedSynthesisJob(
                        batch.addJob(acceptedUpload.fileId()),
                        acceptedUpload.rootContext()
                ))
                .toList();
        synthesisBatchRepository.save(batch);

        List<SynthesisJob> jobs = batch.getJobs();
        if (!jobs.isEmpty()) {
            synthesisJobRepository.saveAll(jobs);
            record.incrementUsage(batch.getAcceptedCount());
            revision.incrementUsage(batch.getAcceptedCount());
            dispatchAfterCommit(acceptedJobs, input.overwrite());
        }

        List<SynthesisJobOutput> items = jobs.stream()
                .map(this::toJobOutput)
                .toList();

        return new SynthesisBatchStartOutput(
                batch.getId(),
                batch.getRequestedCount(),
                batch.getAcceptedCount(),
                items,
                failed
        );
    }

    private void dispatchAfterCommit(List<AcceptedSynthesisJob> acceptedJobs, boolean overwrite) {
        String schemaName = TenantContextHolder.getCurrentSchema();
        Runnable dispatch = () -> {
            for (AcceptedSynthesisJob acceptedJob : acceptedJobs) {
                synthesisAsyncExecutionService.runJobAsync(
                        acceptedJob.job().getId(),
                        schemaName,
                        acceptedJob.rootContext(),
                        overwrite
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }

        dispatch.run();
    }

    private void validateRootContext(MappingScope scope, Map<String, String> rootContext) {
        boolean hasRootContext = rootContext != null && !rootContext.isEmpty();
        if (scope == MappingScope.ROOT_BOM && !hasRootContext) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "이 매핑은 ROOT_BOM입니다. root_context를 지정해주세요."
            );
        }
        if ((scope == MappingScope.PART_LIST || scope == MappingScope.FULL_BOM) && hasRootContext) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "이 매핑은 root_context가 필요하지 않습니다."
            );
        }
    }

    private boolean isProjectOwnedFile(File file, UUID projectId) {
        return "project".equals(file.getOwnerType()) && projectId.equals(file.getOwnerId());
    }

    private SynthesisJobOutput toJobOutput(SynthesisJob job) {
        return new SynthesisJobOutput(
                job.getId(),
                job.getMappingId(),
                job.getFileId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getNodesCreated(),
                job.getRelationshipsCreated(),
                parseErrors(job.getErrors()),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private List<String> parseErrors(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(
                    raw,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            if (parsed == null) {
                return List.of();
            }
            return parsed.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        } catch (JacksonException ignored) {
        }

        List<String> errors = new ArrayList<>();
        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                errors.add(trimmed);
            }
        }
        return errors;
    }

    private String serializeFailures(List<SynthesisBatchFailureOutput> failures) {
        if (failures.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(failures);
        } catch (JacksonException ex) {
            return "[]";
        }
    }

    private record AcceptedSynthesisJob(
            SynthesisJob job,
            Map<String, String> rootContext
    ) {
    }

    private record AcceptedUpload(
            UUID fileId,
            Map<String, String> rootContext
    ) {
    }
}
