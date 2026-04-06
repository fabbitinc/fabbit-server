package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.service.input.CreatePartDraftInput;
import com.fabbitinc.server.application.part.service.input.PartRevisionDecisionInput;
import com.fabbitinc.server.application.part.service.input.UpdatePartRevisionInput;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionDraftChanges;
import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionHistorySourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PartRevisionService {

    private static final Pattern NUMERIC_REVISION_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ALPHA_REVISION_PATTERN = Pattern.compile("^[A-Za-z]+$");

    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PropertyApi propertyApi;
    private final ObjectMapper objectMapper;

    public PartRevision createDraft(CreatePartDraftInput input, UUID actorId) {
        try {
            Part part = getRequiredPart(input.partId());
            part.assertNotObsolete();
            PartRevision baseRevision = getRequiredRevision(input.partId(), input.baseRevisionId());
            baseRevision.assertDraftCreationAllowed();

            PartRevision draft = PartRevision.createDraft(part, baseRevision.getId(), baseRevision.getName(), actorId);
            draft.copyEditableFieldsFrom(baseRevision);
            draft.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CREATED,
                    PartRevisionHistorySourceType.USER,
                    null,
                    serializeReasonPayload(input.reason())
            );
            return partRevisionRepository.save(draft);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision updateDraft(UpdatePartRevisionInput input, UUID actorId) {
        try {
            PartRevision revision = getRequiredDraft(input.partId(), input.revisionId());
            PartRevisionDraftChanges changes = toDraftChanges(input);
            if (!changes.hasAnyChange()) {
                return revision;
            }

            revision.editDraft(changes, actorId);
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision approveDraft(PartRevisionDecisionInput input, UUID actorId) {
        throw new AppException(
                ErrorCode.PART_WORKFLOW_POLICY_FORBIDDEN,
                "직접 승인 모드에서는 승인 단계를 사용하지 않습니다. 초안을 바로 릴리즈해 주세요"
        );
    }

    public PartRevision releaseDraft(PartRevisionDecisionInput input, UUID actorId) {
        try {
            requireChangeReason(input.reason());
            Part part = getRequiredPartForUpdate(input.partId());
            PartRevision draft = getRequiredDraft(input.partId(), input.revisionId());
            assertLatestOfficialBase(part, draft);
            return releaseDraftInternal(
                    part,
                    draft,
                    actorId,
                    PartRevisionHistorySourceType.USER,
                    null,
                    serializeReleasePayload(input.reason())
            );
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision cancelDraft(PartRevisionDecisionInput input, UUID actorId) {
        try {
            requireChangeReason(input.reason());
            PartRevision draft = getRequiredDraft(input.partId(), input.revisionId());
            draft.cancel(actorId);
            draft.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CANCELED,
                    PartRevisionHistorySourceType.USER,
                    null,
                    serializeReasonPayload(input.reason())
            );
            return draft;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision releaseDraftFromEngineeringChange(
            PartRevision draft,
            UUID actorId,
            UUID engineeringChangeId
    ) {
        try {
            Part part = getRequiredPartForUpdate(draft.getPartId());
            assertLatestOfficialBase(part, draft);
            return releaseDraftInternal(
                    part,
                    draft,
                    actorId,
                    PartRevisionHistorySourceType.ENGINEERING_CHANGE,
                    engineeringChangeId,
                    serializeReleasePayload(null)
            );
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision cancelFromEngineeringChange(
            PartRevision revision,
            UUID actorId,
            UUID engineeringChangeId
    ) {
        try {
            Part part = getRequiredPartForUpdate(revision.getPartId());
            clearCurrentApprovedIfMatches(part, revision.getId());
            revision.cancel(actorId);
            revision.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CANCELED,
                    PartRevisionHistorySourceType.ENGINEERING_CHANGE,
                    engineeringChangeId,
                    "{}"
            );
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision getRequiredEditableRevision(UUID partId, UUID revisionId) {
        PartRevision revision = getRequiredRevision(partId, revisionId);
        revision.assertDraftEditable();
        return revision;
    }

    private PartRevision releaseDraftInternal(
            Part part,
            PartRevision draft,
            UUID actorId,
            PartRevisionHistorySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        String revisionCode = resolveNextRevisionCode(part);
        supersedeCurrentReleasedIfNeeded(part, draft.getId(), actorId, sourceType, sourceRefId, payload);
        draft.release(revisionCode, actorId);
        part.assignCurrentReleasedRevision(draft.getId());
        draft.recordHistory(
                actorId,
                PartRevisionHistoryActionType.RELEASED,
                sourceType,
                sourceRefId,
                payload
        );
        return draft;
    }

    private PartRevisionDraftChanges toDraftChanges(UpdatePartRevisionInput input) {
        return new PartRevisionDraftChanges(
                input.name(),
                input.nameSet(),
                input.material(),
                input.materialSet(),
                input.unit(),
                input.unitSet(),
                input.description(),
                input.descriptionSet(),
                input.leadTimeDays(),
                input.leadTimeDaysSet(),
                input.extendedPropertiesSet() ? serializeProperties(validateExtendedProperties(input.extendedProperties())) : null,
                input.extendedPropertiesSet()
        );
    }

    private Map<String, Object> validateExtendedProperties(Map<String, Object> extendedProperties) {
        return propertyApi.validateExtendedProperties(PropertyOwnerType.PART, extendedProperties);
    }

    private PartRevision getRequiredRevision(UUID partId, UUID revisionId) {
        return partRevisionRepository.findByIdAndPartId(revisionId, partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partId, revisionId)
                ));
    }

    private PartRevision getRequiredDraft(UUID partId, UUID revisionId) {
        return partRevisionRepository.findByIdAndPartId(revisionId, partId)
                .filter(revision -> revision.getStatus() == PartRevisionStatus.DRAFT)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partId, revisionId)
                ));
    }

    private Part getRequiredPart(UUID partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partId)
                ));
    }

    private Part getRequiredPartForUpdate(UUID partId) {
        return partRepository.findByIdForUpdate(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partId)
                ));
    }

    private void requireChangeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "변경 사유는 필수입니다");
        }
    }

    private void assertLatestOfficialBase(Part part, PartRevision draft) {
        PartRevision latestOfficialRevision = resolveLatestOfficialRevision(part);
        UUID latestOfficialRevisionId = latestOfficialRevision == null ? null : latestOfficialRevision.getId();

        if (draft.getBaseRevisionId() == null) {
            if (latestOfficialRevisionId != null) {
                throw new AppException(
                        ErrorCode.CONFLICT,
                        "이미 공식 리비전이 존재합니다. 최신 공식 리비전을 기준으로 새 초안을 생성해 주세요"
                );
            }
            return;
        }

        if (latestOfficialRevisionId != null && !latestOfficialRevisionId.equals(draft.getBaseRevisionId())) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "최신 공식 리비전을 기준으로 다시 초안을 만들어야 합니다"
            );
        }
    }

    private void supersedeCurrentReleasedIfNeeded(
            Part part,
            UUID nextRevisionId,
            UUID actorId,
            PartRevisionHistorySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        UUID currentReleasedRevisionId = part.getCurrentReleasedRevisionId();
        if (currentReleasedRevisionId == null || currentReleasedRevisionId.equals(nextRevisionId)) {
            return;
        }
        PartRevision currentReleasedRevision = getRequiredRevision(currentReleasedRevisionId);
        currentReleasedRevision.markSuperseded(actorId);
        currentReleasedRevision.recordHistory(
                actorId,
                PartRevisionHistoryActionType.SUPERSEDED,
                sourceType,
                sourceRefId,
                payload
        );
    }

    private PartRevision getRequiredRevision(UUID revisionId) {
        return partRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s'을(를) 찾을 수 없습니다".formatted(revisionId)
                ));
    }

    private String resolveNextRevisionCode(Part part) {
        PartRevision latestOfficialRevision = resolveLatestOfficialRevision(part);
        if (latestOfficialRevision == null || latestOfficialRevision.getRevisionCode() == null) {
            return "1";
        }
        return incrementRevisionCode(latestOfficialRevision.getRevisionCode());
    }

    private PartRevision resolveLatestOfficialRevision(Part part) {
        UUID revisionId = part.getCurrentReleasedRevisionId();
        if (revisionId != null) {
            return getRequiredRevision(revisionId);
        }
        return partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()).stream()
                .filter(revision -> revision.getRevisionCode() != null && !revision.getRevisionCode().isBlank())
                .findFirst()
                .orElse(null);
    }

    private String incrementRevisionCode(String currentRevisionCode) {
        String trimmed = currentRevisionCode.trim();
        if (NUMERIC_REVISION_PATTERN.matcher(trimmed).matches()) {
            return Long.toString(Long.parseLong(trimmed) + 1L);
        }
        if (ALPHA_REVISION_PATTERN.matcher(trimmed).matches()) {
            return incrementAlphabeticRevisionCode(trimmed.toUpperCase());
        }
        throw new AppException(
                ErrorCode.INVALID_STATE,
                "자동 리비전 코드 증분을 지원하지 않는 형식입니다: " + currentRevisionCode
        );
    }

    private String incrementAlphabeticRevisionCode(String value) {
        char[] chars = value.toCharArray();
        int index = chars.length - 1;
        while (index >= 0 && chars[index] == 'Z') {
            chars[index] = 'A';
            index--;
        }
        if (index < 0) {
            return "A" + new String(chars);
        }
        chars[index] = (char) (chars[index] + 1);
        return new String(chars);
    }

    private void clearCurrentApprovedIfMatches(Part part, UUID revisionId) {
        // 승인 포인터를 사용하지 않는 구조라 no-op으로 둔다.
    }

    private String serializeReasonPayload(String reason) {
        if (reason == null || reason.isBlank()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(Map.of("reason", reason.trim()));
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 이력을 직렬화할 수 없습니다");
        }
    }

    private String serializeReleasePayload(String reason) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reason", reason == null ? "" : reason.trim());
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "릴리즈 이력을 직렬화할 수 없습니다");
        }
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
                    PartRevision.CODE_PART_REVISION_DRAFT_CODE_FORBIDDEN,
                    PartRevision.CODE_PART_REVISION_RELEASABLE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_SUPERSEDE_INVALID_STATE ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
