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

    public PartRevision getRequiredDraft(String partNumber, UUID draftId) {
        return partRevisionRepository.findByIdAndPartNumber(draftId, partNumber)
                .filter(revision -> revision.getStatus() == com.fabbitinc.server.domain.part.model.PartRevisionStatus.DRAFT
                        || revision.getStatus() == com.fabbitinc.server.domain.part.model.PartRevisionStatus.IN_REVIEW)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, draftId)
                ));
    }

    public UUID getRequiredPartId(String partNumber, String revisionCode) {
        return getRequiredRevision(partNumber, revisionCode).getPartId();
    }

    public UUID getRequiredDraftPartId(String partNumber, UUID draftId) {
        return getRequiredDraft(partNumber, draftId).getPartId();
    }
}
