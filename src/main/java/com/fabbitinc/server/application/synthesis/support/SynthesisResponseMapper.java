package com.fabbitinc.server.application.synthesis.support;

import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SynthesisResponseMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<SynthesisBatchFailure>> FAILURE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

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
        try {
            List<String> parsed = objectMapper.readValue(raw, STRING_LIST_TYPE);
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            return parsed.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        } catch (JacksonException ignored) {
            // 기존 개행 문자열 포맷과의 호환을 위해 fallback 처리
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
        try {
            List<SynthesisBatchFailure> parsed = objectMapper.readValue(raw, FAILURE_LIST_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (JacksonException ignored) {
            // 기존 탭 구분 문자열 포맷과의 호환을 위해 fallback 처리
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
        try {
            return objectMapper.writeValueAsString(failures);
        } catch (JacksonException ex) {
            return "[]";
        }
    }
}
