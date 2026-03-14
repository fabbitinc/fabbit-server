package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartRevisionRouteService {

    private final PartRevisionRepository partRevisionRepository;

    public PartRevision getRequiredRevision(String partNumber, String revisionCode) {
        return partRevisionRepository.findByPartNumberAndRevisionCode(partNumber, revisionCode)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, revisionCode)
                ));
    }

    public PartRevision getRequiredDraft(String partNumber, String baseRevisionCode, String draftKey) {
        return (baseRevisionCode == null || baseRevisionCode.isBlank()
                ? partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull(partNumber, draftKey)
                : findRevisionScopedDraft(partNumber, baseRevisionCode, draftKey))
                .filter(revision -> revision.getStatus() == com.fabbitinc.server.domain.part.model.PartRevisionStatus.DRAFT
                        || revision.getStatus() == com.fabbitinc.server.domain.part.model.PartRevisionStatus.IN_REVIEW)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, draftKey)
                ));
    }

    public UUID getRequiredPartId(String partNumber, String revisionCode) {
        return getRequiredRevision(partNumber, revisionCode).getPartId();
    }

    public UUID getRequiredRevisionId(String partNumber, String revisionCode) {
        return getRequiredRevision(partNumber, revisionCode).getId();
    }

    public UUID getRequiredDraftPartId(String partNumber, String baseRevisionCode, String draftKey) {
        return getRequiredDraft(partNumber, baseRevisionCode, draftKey).getPartId();
    }

    private java.util.Optional<PartRevision> findRevisionScopedDraft(String partNumber, String baseRevisionCode, String draftKey) {
        PartRevision baseRevision = getRequiredRevision(partNumber, baseRevisionCode);
        return partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionId(
                partNumber,
                draftKey,
                baseRevision.getId()
        );
    }
}
