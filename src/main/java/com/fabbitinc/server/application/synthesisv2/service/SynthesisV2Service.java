package com.fabbitinc.server.application.synthesisv2.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesisv2.service.input.StartSynthesisV2Input;
import com.fabbitinc.server.application.synthesisv2.service.input.SynthesisV2UploadInput;
import com.fabbitinc.server.application.synthesisv2.service.output.SynthesisV2BatchFailureOutput;
import com.fabbitinc.server.application.synthesisv2.service.output.SynthesisV2BatchStartOutput;
import com.fabbitinc.server.application.synthesisv2.service.output.SynthesisV2JobOutput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RecordRepository;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Batch;
import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Job;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2BatchRepository;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2JobRepository;
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
public class SynthesisV2Service {

    private final MappingV2RecordRepository mappingV2RecordRepository;
    private final MappingV2RevisionRepository mappingV2RevisionRepository;
    private final FileRepository fileRepository;
    private final SynthesisV2BatchRepository synthesisV2BatchRepository;
    private final SynthesisV2JobRepository synthesisV2JobRepository;
    private final SynthesisV2AsyncExecutionService synthesisV2AsyncExecutionService;
    private final ObjectMapper objectMapper;

    public SynthesisV2BatchStartOutput startSynthesis(StartSynthesisV2Input input) {
        MappingV2Record record = mappingV2RecordRepository.findByIdAndActiveTrue(input.mappingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑을 찾을 수 없습니다"));

        MappingV2Revision revision = mappingV2RevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑 리비전을 찾을 수 없습니다"));

        List<SynthesisV2BatchFailureOutput> failed = new ArrayList<>();
        List<AcceptedUpload> acceptedUploads = new ArrayList<>();

        for (SynthesisV2UploadInput item : input.uploads()) {
            File file = fileRepository.findByIdAndDeletedAtIsNull(item.fileId()).orElse(null);
            if (file == null) {
                failed.add(new SynthesisV2BatchFailureOutput(item.fileId(), "파일을 찾을 수 없습니다"));
                continue;
            }
            if (file.getStatus() != FileStatus.UPLOADED) {
                failed.add(new SynthesisV2BatchFailureOutput(item.fileId(), "업로드가 완료되지 않은 파일입니다"));
                continue;
            }
            if (input.projectId() != null && !isProjectOwnedFile(file, input.projectId())) {
                failed.add(new SynthesisV2BatchFailureOutput(item.fileId(), "해당 프로젝트에 속하지 않은 파일입니다"));
                continue;
            }
            Map<String, String> rootContext = item.rootContext() == null ? Map.of() : item.rootContext();
            acceptedUploads.add(new AcceptedUpload(file.getId(), rootContext));
        }

        SynthesisV2Batch batch = SynthesisV2Batch.create(
                input.projectId(),
                record.getId(),
                input.requestedBy(),
                input.uploads().size(),
                serializeFailures(failed)
        );
        List<AcceptedSynthesisJob> acceptedJobs = acceptedUploads.stream()
                .map(acceptedUpload -> new AcceptedSynthesisJob(
                        batch.addJob(acceptedUpload.fileId()),
                        acceptedUpload.rootContext()
                ))
                .toList();
        synthesisV2BatchRepository.save(batch);

        List<SynthesisV2Job> jobs = batch.getJobs();
        if (!jobs.isEmpty()) {
            synthesisV2JobRepository.saveAll(jobs);
            record.incrementUsage(batch.getAcceptedCount());
            revision.incrementUsage(batch.getAcceptedCount());
            dispatchAfterCommit(acceptedJobs, input.overwrite());
        }

        List<SynthesisV2JobOutput> items = jobs.stream()
                .map(this::toJobOutput)
                .toList();

        return new SynthesisV2BatchStartOutput(
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
                synthesisV2AsyncExecutionService.runJobAsync(
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

    private boolean isProjectOwnedFile(File file, UUID projectId) {
        return "project".equals(file.getOwnerType()) && projectId.equals(file.getOwnerId());
    }

    private SynthesisV2JobOutput toJobOutput(SynthesisV2Job job) {
        return new SynthesisV2JobOutput(
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
            return parsed == null ? List.of() : parsed.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        } catch (JacksonException ignored) {
        }
        return List.of();
    }

    private String serializeFailures(List<SynthesisV2BatchFailureOutput> failures) {
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
            SynthesisV2Job job,
            Map<String, String> rootContext
    ) {
    }

    private record AcceptedUpload(
            UUID fileId,
            Map<String, String> rootContext
    ) {
    }
}
