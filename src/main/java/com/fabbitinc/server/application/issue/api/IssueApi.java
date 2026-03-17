package com.fabbitinc.server.application.issue.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueApi {

    private final IssueRepository issueRepository;

    public boolean existsIssue(UUID issueId) {
        return issueRepository.existsById(issueId);
    }

    public int getNextIssueNumberSeed() {
        return issueRepository.findTopByOrderByNumberDesc()
                .map(issue -> issue.getNumber() + 1)
                .orElse(1);
    }

    public void validateIssueIds(Collection<UUID> issueIds) {
        if (issueIds == null) {
            return;
        }
        for (UUID issueId : issueIds) {
            if (issueRepository.findById(issueId).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "Issue '" + issueId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    public Map<UUID, IssueSnapshot> getIssueSnapshotMap(Set<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, IssueSnapshot> result = new LinkedHashMap<>();
        issueRepository.findAllById(issueIds).forEach(issue -> result.put(
                issue.getId(),
                new IssueSnapshot(issue.getId(), issue.getNumber(), issue.getTitle(), issue.getState())
        ));
        return result;
    }
}
