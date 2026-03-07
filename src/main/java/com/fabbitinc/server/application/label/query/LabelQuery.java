package com.fabbitinc.server.application.label.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.query.condition.LabelListCondition;
import com.fabbitinc.server.application.label.query.condition.LabelLookupCondition;
import com.fabbitinc.server.application.label.query.result.LabelListResult;
import com.fabbitinc.server.application.label.query.result.LabelLookupItemResult;
import com.fabbitinc.server.application.label.query.result.LabelLookupResult;
import com.fabbitinc.server.application.label.query.result.LabelResult;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelRepository labelRepository;

    public LabelListResult list(LabelListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        List<LabelResult> items = labelRepository.findAllByOrderByNameAsc().stream()
                .map(this::toLabelResult)
                .toList();
        return new LabelListResult(items.size(), items);
    }

    public LabelLookupResult lookup(LabelLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();
        String normalizedSearch = normalizeSearch(condition.search());
        List<Label> labels = normalizedSearch == null
                ? labelRepository.findAllByOrderByNameAsc(PageRequest.of(0, condition.limit()))
                : labelRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                        normalizedSearch,
                        PageRequest.of(0, condition.limit())
                );
        List<LabelLookupItemResult> items = labels.stream()
                .map(label -> new LabelLookupItemResult(
                        label.getId(),
                        label.getName(),
                        label.getColor()
                ))
                .toList();
        return new LabelLookupResult(items);
    }

    private LabelResult toLabelResult(Label label) {
        return new LabelResult(
                label.getId(),
                label.getName(),
                label.getDescription(),
                label.getColor(),
                label.getCreatedAt(),
                label.getCreatedBy()
        );
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
