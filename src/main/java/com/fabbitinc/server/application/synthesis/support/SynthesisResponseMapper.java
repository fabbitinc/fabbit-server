package com.fabbitinc.server.application.synthesis.support;

import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SynthesisResponseMapper {

    public SynthesisJobResponse toJobResponse(SynthesisJob job) {
        return new SynthesisJobResponse(
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

    public List<String> parseErrors(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return List.of();
        }
        String[] lines = raw.split("\\R");
        List<String> errors = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                errors.add(trimmed);
            }
        }
        return errors;
    }

    public List<SynthesisBatchFailure> parseFailures(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return List.of();
        }
        List<SynthesisBatchFailure> failures = new ArrayList<>();
        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String[] tokens = line.split("\\t", 2);
            if (tokens.length != 2) {
                continue;
            }
            try {
                failures.add(new SynthesisBatchFailure(UUID.fromString(tokens[0]), tokens[1]));
            } catch (Exception ignored) {
                // 형식 불일치 항목은 무시
            }
        }
        return failures;
    }

    public String serializeFailures(List<SynthesisBatchFailure> failures) {
        if (failures.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder();
        for (SynthesisBatchFailure failure : failures) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(failure.fileId()).append('\t').append(failure.reason());
        }
        return builder.toString();
    }
}
