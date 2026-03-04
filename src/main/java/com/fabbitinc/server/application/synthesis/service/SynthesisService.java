package com.fabbitinc.server.application.synthesis.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.application.synthesis.dto.request.SynthesisUploadItem;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.application.synthesis.support.SynthesisResponseMapper;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisBatchRepository;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SynthesisService {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final FileRepository fileRepository;
    private final SynthesisBatchRepository synthesisBatchRepository;
    private final SynthesisJobRepository synthesisJobRepository;
    private final SynthesisResponseMapper synthesisResponseMapper;

    public SynthesisBatchStartResponse startSynthesis(SynthesisStartRequest request) {
        MappingRecord record = mappingRecordRepository.findByIdAndActiveTrue(request.mappingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));

        MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑 리비전을 찾을 수 없습니다"));

        List<SynthesisBatchFailure> failed = new ArrayList<>();
        List<SynthesisJob> acceptedJobs = new ArrayList<>();

        for (SynthesisUploadItem item : request.uploads()) {
            validateRootContext(record.getScope(), item.rootContext());

            File file = fileRepository.findByIdAndDeletedAtIsNull(item.fileId()).orElse(null);
            if (file == null) {
                failed.add(new SynthesisBatchFailure(item.fileId(), "파일을 찾을 수 없습니다"));
                continue;
            }
            if (file.getStatus() != FileStatus.UPLOADED) {
                failed.add(new SynthesisBatchFailure(item.fileId(), "업로드가 완료되지 않은 파일입니다"));
                continue;
            }
            if (request.projectId() != null && !isProjectOwnedFile(file, request.projectId())) {
                failed.add(new SynthesisBatchFailure(item.fileId(), "해당 프로젝트에 속하지 않은 파일입니다"));
                continue;
            }

            SynthesisJob job = new SynthesisJob(record.getId(), file.getId());
            acceptedJobs.add(job);
        }

        SynthesisBatch batch = new SynthesisBatch(
                request.projectId(),
                record.getId(),
                request.uploads().size(),
                acceptedJobs.size(),
                synthesisResponseMapper.serializeFailures(failed)
        );
        synthesisBatchRepository.save(batch);

        for (SynthesisJob job : acceptedJobs) {
            job.assignBatch(batch.getId());
        }
        if (!acceptedJobs.isEmpty()) {
            synthesisJobRepository.saveAll(acceptedJobs);
            record.incrementUsage(acceptedJobs.size());
            revision.incrementUsage(acceptedJobs.size());
        }

        List<SynthesisJobResponse> items = acceptedJobs.stream()
                .map(synthesisResponseMapper::toJobResponse)
                .toList();

        return new SynthesisBatchStartResponse(
                batch.getId(),
                batch.getRequestedCount(),
                batch.getAcceptedCount(),
                items,
                failed
        );
    }

    private void validateRootContext(String scope, Map<String, String> rootContext) {
        boolean hasRootContext = rootContext != null && !rootContext.isEmpty();
        if ("ROOT_BOM".equals(scope) && !hasRootContext) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "이 매핑은 ROOT_BOM입니다. root_context를 지정해주세요."
            );
        }
        if (("PART_LIST".equals(scope) || "FULL_BOM".equals(scope)) && hasRootContext) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "이 매핑은 root_context가 필요하지 않습니다."
            );
        }
    }

    private boolean isProjectOwnedFile(File file, UUID projectId) {
        return "project".equals(file.getOwnerType()) && projectId.equals(file.getOwnerId());
    }
}
