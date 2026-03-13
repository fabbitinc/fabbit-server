package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.service.input.CreatePartDraftInput;
import com.fabbitinc.server.application.part.service.input.UpdatePartRevisionInput;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PartRevisionService {

    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final ObjectMapper objectMapper;

    public PartRevision createDraft(CreatePartDraftInput input, UUID actorId) {
        try {
            PartRevision baseRevision = getRequiredRevision(input.partNumber(), input.baseRevisionCode());
            baseRevision.assertDraftCreationAllowed();

            Part part = getRequiredPart(baseRevision.getPartId());
            PartRevision draft = PartRevision.createDraft(part, baseRevision.getId(), baseRevision.getName());
            draft.copyEditableFieldsFrom(baseRevision);
            draft.recordActivity(actorId, PartRevisionActivityActionType.CREATED, PartRevisionActivitySourceType.API, null, "{}");
            return partRevisionRepository.save(draft);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision updateDraft(UpdatePartRevisionInput input, UUID actorId) {
        try {
            PartRevision revision = getRequiredDraft(input.partNumber(), input.draftId());
            revision.assertDraftEditable();

            if (!input.hasAnyFieldSet()) {
                return revision;
            }

            applyUpdateInput(revision, input);
            revision.recordActivity(actorId, PartRevisionActivityActionType.EDITED, PartRevisionActivitySourceType.API, null, "{}");
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    private void applyUpdateInput(PartRevision revision, UpdatePartRevisionInput input) {
        if (input.nameSet()) {
            revision.changeName(input.name());
        }
        if (input.materialSet()) {
            revision.changeMaterial(input.material());
        }
        if (input.unitSet()) {
            revision.changeUnit(input.unit());
        }
        if (input.descriptionSet()) {
            revision.changeDescription(input.description());
        }
        if (input.categorySet()) {
            revision.changeCategory(input.category());
        }
        if (input.phantomSet()) {
            applyPhantom(revision, input.phantom());
        }
        if (input.leadTimeDaysSet()) {
            revision.changeLeadTimeDays(input.leadTimeDays());
        }
        if (input.extendedPropertiesSet()) {
            revision.changeExtendedProperties(serializeProperties(input.extendedProperties()));
        }
    }

    private void applyPhantom(PartRevision revision, Boolean phantom) {
        if (phantom == null) {
            revision.clearPhantomFlag();
            return;
        }
        if (Boolean.TRUE.equals(phantom)) {
            revision.markPhantom();
            return;
        }
        revision.markReal();
    }

    private PartRevision getRequiredRevision(String partNumber, String revisionCode) {
        return partRevisionRepository.findByPartNumberAndRevisionCode(partNumber, revisionCode)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, revisionCode)
                ));
    }

    private PartRevision getRequiredDraft(String partNumber, UUID draftId) {
        return partRevisionRepository.findByIdAndPartNumber(draftId, partNumber)
                .filter(revision -> revision.getStatus() == PartRevisionStatus.DRAFT)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, draftId)
                ));
    }

    private Part getRequiredPart(UUID partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partId)
                ));
    }

    private String serializeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties == null ? Map.of() : properties);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "extended_properties를 직렬화할 수 없습니다");
        }
    }

    private AppException toAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case PartRevision.CODE_PART_REVISION_NAME_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_CATEGORY_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_MATERIAL_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_UNIT_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_REQUIRED,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_CODE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_CODE_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_CODE_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            case PartRevision.CODE_PART_REVISION_DRAFT_REQUIRED,
                    PartRevision.CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_DRAFT_CODE_FORBIDDEN ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
