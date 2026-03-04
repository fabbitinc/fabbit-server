package com.fabbitinc.server.application.label.query;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.label.dto.response.LabelListResponse;
import com.fabbitinc.server.application.label.dto.response.LabelLookupItemResponse;
import com.fabbitinc.server.application.label.dto.response.LabelLookupResponse;
import com.fabbitinc.server.application.label.dto.response.LabelResponse;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LabelQuery {

    private final AuthTokenParser authTokenParser;
    private final LabelRepository labelRepository;

    @Transactional(readOnly = true)
    public LabelListResponse listLabels(String authorizationHeader) {
        authTokenParser.requireAuth(authorizationHeader);
        List<LabelResponse> items = labelRepository.findAllByOrderByNameAsc().stream()
                .map(this::toLabelResponse)
                .toList();
        return new LabelListResponse(items.size(), items);
    }

    @Transactional(readOnly = true)
    public LabelLookupResponse lookupLabels(
            String authorizationHeader,
            String search,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);
        List<LabelLookupItemResponse> items = labelRepository.lookupLabels(
                        normalizeSearch(search),
                        PageRequest.of(0, limit)
                ).stream()
                .map(label -> new LabelLookupItemResponse(
                        label.getId(),
                        label.getName(),
                        label.getColor()
                ))
                .toList();
        return new LabelLookupResponse(items);
    }

    private LabelResponse toLabelResponse(Label label) {
        return new LabelResponse(
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
