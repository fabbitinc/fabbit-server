package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.service.input.CreatePartDraftInput;
import com.fabbitinc.server.application.part.service.input.PartRevisionDecisionInput;
import com.fabbitinc.server.application.part.service.input.ReleasePartRevisionInput;
import com.fabbitinc.server.application.part.service.input.UpdatePartRevisionInput;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionHistorySourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionDraftChanges;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;

    public PartRevision createDraft(CreatePartDraftInput input, UUID actorId) {
        try {
            PartRevision baseRevision = getRequiredRevision(input.partNumber(), input.baseRevisionCode());
            baseRevision.assertDraftCreationAllowed();

            Part part = getRequiredPart(baseRevision.getPartId());
            String draftKey = resolveNextDraftKey(part.getId(), baseRevision.getId());
            PartRevision draft = PartRevision.createDraft(part, draftKey, baseRevision.getId(), baseRevision.getName(), actorId);
            draft.copyEditableFieldsFrom(baseRevision);
            draft.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CREATED,
                    PartRevisionHistorySourceType.UI,
                    null,
                    serializeReasonPayload(input.reason())
            );
            return partRevisionRepository.save(draft);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    private String resolveNextDraftKey(UUID partId, UUID baseRevisionId) {
        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId);
        int next = revisions.stream()
                .filter(revision -> baseRevisionId.equals(revision.getBaseRevisionId()))
                .map(PartRevision::getDraftKey)
                .filter(value -> value != null && value.matches("D\\d+"))
                .map(value -> Integer.parseInt(value.substring(1)))
                .max(Comparator.naturalOrder())
                .orElse(0);
        return "D" + (next + 1);
    }

    public PartRevision updateDraft(UpdatePartRevisionInput input, UUID actorId) {
        try {
            PartRevision revision = getRequiredDraft(input.partNumber(), input.baseRevisionCode(), input.draftKey());
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
            Part part = getRequiredPartForUpdate(input.partNumber());
            PartRevision draft = getRequiredDraft(input.partNumber(), input.baseRevisionCode(), input.draftKey());
            assertLatestOfficialBase(part, draft);
            return releaseDraftInternal(
                    part,
                    draft,
                    actorId,
                    PartRevisionHistorySourceType.UI,
                    null,
                    serializeTransitionPayload("RELEASED", resolveNextRevisionCode(part), input.reason())
            );
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision cancelDraft(PartRevisionDecisionInput input, UUID actorId) {
        try {
            requireChangeReason(input.reason());
            PartRevision draft = getRequiredDraft(input.partNumber(), input.baseRevisionCode(), input.draftKey());
            String canceledDraftKey = draft.getDraftKey();
            draft.cancel(actorId);
            draft.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CANCELED,
                    PartRevisionHistorySourceType.UI,
                    null,
                    serializeTransitionPayload("CANCELED", canceledDraftKey, input.reason())
            );
            return draft;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision releaseRevision(ReleasePartRevisionInput input, UUID actorId) {
        throw new AppException(
                ErrorCode.PART_WORKFLOW_POLICY_FORBIDDEN,
                "직접 승인 모드에서는 승인된 리비전 릴리즈를 사용하지 않습니다. 초안을 바로 릴리즈해 주세요"
        );
    }

    public PartRevision releaseDraftFromEngineeringChange(
            PartRevision draft,
            UUID actorId,
            UUID engineeringChangeId,
            int engineeringChangeNumber,
            String engineeringChangeTitle
    ) {
        try {
            Part part = getRequiredPartForUpdate(draft.getPartNumber());
            assertLatestOfficialBase(part, draft);
            return releaseDraftInternal(
                    part,
                    draft,
                    actorId,
                    PartRevisionHistorySourceType.ENGINEERING_CHANGE,
                    engineeringChangeId,
                    serializeEngineeringChangePayload(
                            "RELEASED",
                            resolveNextRevisionCode(part),
                            engineeringChangeNumber,
                            engineeringChangeTitle
                    )
            );
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision releaseApprovedFromEngineeringChange(
            PartRevision revision,
            UUID actorId,
            UUID engineeringChangeId,
            int engineeringChangeNumber,
            String engineeringChangeTitle
    ) {
        return releaseDraftFromEngineeringChange(
                revision,
                actorId,
                engineeringChangeId,
                engineeringChangeNumber,
                engineeringChangeTitle
        );
    }

    public PartRevision cancelFromEngineeringChange(
            PartRevision revision,
            UUID actorId,
            UUID engineeringChangeId,
            int engineeringChangeNumber,
            String engineeringChangeTitle
    ) {
        try {
            requireEngineeringChangeRevision(revision);
            Part part = getRequiredPartForUpdate(revision.getPartNumber());
            String canceledIdentifier = revision.getRevisionCode() == null ? revision.getDraftKey() : revision.getRevisionCode();
            clearCurrentApprovedIfMatches(part, revision.getId());
            revision.cancel(actorId);
            revision.recordHistory(
                    actorId,
                    PartRevisionHistoryActionType.CANCELED,
                    PartRevisionHistorySourceType.ENGINEERING_CHANGE,
                    engineeringChangeId,
                    serializeEngineeringChangePayload(
                            "CANCELED",
                            canceledIdentifier,
                            engineeringChangeNumber,
                            engineeringChangeTitle
                    )
            );
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
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
                input.category(),
                input.categorySet(),
                input.phantom(),
                input.phantomSet(),
                input.leadTimeDays(),
                input.leadTimeDaysSet(),
                input.extendedPropertiesSet() ? serializeProperties(input.extendedProperties()) : null,
                input.extendedPropertiesSet()
        );
    }

    private PartRevision getRequiredRevision(String partNumber, String revisionCode) {
        return partRevisionRepository.findByPartNumberAndRevisionCode(partNumber, revisionCode)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, revisionCode)
                ));
    }

    private PartRevision getRequiredDraft(String partNumber, String baseRevisionCode, String draftKey) {
        Optional<PartRevision> candidate = baseRevisionCode == null || baseRevisionCode.isBlank()
                ? partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull(partNumber, draftKey)
                : findRevisionScopedDraft(partNumber, baseRevisionCode, draftKey);
        return candidate
                .filter(revision -> revision.getStatus() == PartRevisionStatus.DRAFT)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, draftKey)
                ));
    }

    private Optional<PartRevision> findRevisionScopedDraft(String partNumber, String baseRevisionCode, String draftKey) {
        PartRevision baseRevision = getRequiredRevision(partNumber, baseRevisionCode);
        return partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionId(
                partNumber,
                draftKey,
                baseRevision.getId()
        );
    }

    private Part getRequiredPart(UUID partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partId)
                ));
    }

    private Part getRequiredPartForUpdate(String partNumber) {
        return partRepository.findByPartNumberForUpdate(partNumber)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partNumber)
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

    private String serializeTransitionPayload(String action, String revisionCode, String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "action", action,
                    "revisionCode", revisionCode,
                    "reason", reason.trim()
            ));
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 이력을 직렬화할 수 없습니다");
        }
    }

    private void requireEngineeringChangeRevision(PartRevision revision) {
        if (revision.getEngineeringChangeId() == null) {
            throw new AppException(ErrorCode.CONFLICT, "EngineeringChange에 연결된 리비전만 처리할 수 있습니다");
        }
    }

    private void clearCurrentApprovedIfMatches(Part part, UUID revisionId) {
        // 승인 포인터를 사용하지 않는 구조라 no-op으로 둔다.
    }

    private String serializeEngineeringChangePayload(
            String action,
            String revisionCode,
            int engineeringChangeNumber,
            String engineeringChangeTitle
    ) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "action", action,
                    "revisionCode", revisionCode,
                    "engineeringChangeNumber", engineeringChangeNumber,
                    "engineeringChangeTitle", engineeringChangeTitle == null ? "" : engineeringChangeTitle.trim()
            ));
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 이력을 직렬화할 수 없습니다");
        }
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
                    PartRevision.CODE_PART_REVISION_DRAFT_KEY_REQUIRED,
                    PartRevision.CODE_PART_REVISION_DRAFT_KEY_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_DRAFT_KEY_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            case PartRevision.CODE_PART_REVISION_DRAFT_REQUIRED,
                    PartRevision.CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_DRAFT_CODE_FORBIDDEN,
                    PartRevision.CODE_PART_REVISION_ENGINEERING_CHANGE_INVALID_STATE,
                    PartRevision.CODE_PART_REVISION_RELEASABLE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_SUPERSEDE_INVALID_STATE ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
            case PartRevision.CODE_PART_REVISION_ENGINEERING_CHANGE_REQUIRED ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
