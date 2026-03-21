package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatToolRegistry {

    private final PartApi partApi;
    private final IssueApi issueApi;

    public List<PartSnapshot> searchPartSnapshots(String keyword, int limit) {
        return partApi.searchPartSnapshots(keyword, limit);
    }

    public List<IssueSnapshot> getIssueSnapshotsByPartIds(Set<UUID> partIds) {
        Set<UUID> issueIds = issueApi.getIssueIdsByPartIds(partIds == null ? Set.of() : partIds);
        return new LinkedHashSet<>(issueApi.getIssueSnapshotMap(issueIds).values()).stream()
                .sorted(Comparator.comparing(IssueSnapshot::number).reversed())
                .toList();
    }
}
