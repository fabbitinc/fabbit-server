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
import com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType;
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
            draft.recordActivity(
                    actorId,
                    PartRevisionActivityActionType.CREATED,
                    PartRevisionActivitySourceType.UI,
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
            revision.recordActivity(actorId, PartRevisionActivityActionType.EDITED, PartRevisionActivitySourceType.UI, null, "{}");
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision approveDraft(PartRevisionDecisionInput input, UUID actorId) {
        try {
            requireChangeReason(input.reason());
            Part part = getRequiredPartForUpdate(input.partNumber());
            PartRevision draft = getRequiredDraft(input.partNumber(), input.baseRevisionCode(), input.draftKey());
            assertLatestOfficialBase(part, draft);

            String revisionCode = resolveNextRevisionCode(part);
            supersedeCurrentApprovedIfNeeded(part, draft.getId(), actorId);
            draft.approve(revisionCode, actorId);
            part.assignCurrentApprovedRevision(draft.getId());
            draft.recordActivity(
                    actorId,
                    PartRevisionActivityActionType.APPROVED,
                    PartRevisionActivitySourceType.UI,
                    null,
                    serializeTransitionPayload("APPROVED", revisionCode, input.reason())
            );
            return draft;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
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
                    PartRevisionActivitySourceType.UI,
                    null,
                    serializeTransitionPayload("RELEASED", resolveNextRevisionCode(part), input.reason())
            );
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision releaseRevision(ReleasePartRevisionInput input, UUID actorId) {
        try {
            requireChangeReason(input.reason());
            Part part = getRequiredPartForUpdate(input.partNumber());
            PartRevision revision = getRequiredRevision(input.partNumber(), input.revisionCode());
            assertReleasableApprovedRevision(part, revision);

            supersedeCurrentReleasedIfNeeded(part, revision.getId(), actorId);
            revision.release(revision.getRevisionCode(), actorId);
            part.assignCurrentApprovedRevision(revision.getId());
            part.assignCurrentReleasedRevision(revision.getId());
            revision.recordActivity(
                    actorId,
                    PartRevisionActivityActionType.RELEASED,
                    PartRevisionActivitySourceType.UI,
                    null,
                    serializeTransitionPayload("RELEASED", revision.getRevisionCode(), input.reason())
            );
            return revision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public void markInReview(PartRevision revision, UUID actorId) {
        try {
            revision.markInReview(actorId);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public void revertToDraft(PartRevision revision, UUID actorId) {
        try {
            revision.revertToDraft(actorId);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
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
                    PartRevisionActivitySourceType.ENGINEERING_CHANGE,
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

    private PartRevision releaseDraftInternal(
            Part part,
            PartRevision draft,
            UUID actorId,
            PartRevisionActivitySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        String revisionCode = resolveNextRevisionCode(part);
        supersedeCurrentApprovedIfNeeded(part, draft.getId(), actorId);
        supersedeCurrentReleasedIfNeeded(part, draft.getId(), actorId);
        draft.release(revisionCode, actorId);
        part.assignCurrentApprovedRevision(draft.getId());
        part.assignCurrentReleasedRevision(draft.getId());
        draft.recordActivity(
                actorId,
                PartRevisionActivityActionType.RELEASED,
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

    private void assertReleasableApprovedRevision(Part part, PartRevision revision) {
        if (revision.getStatus() == PartRevisionStatus.RELEASED) {
            throw new AppException(ErrorCode.INVALID_STATE, "이미 릴리즈된 리비전입니다");
        }
        if (revision.getStatus() != PartRevisionStatus.APPROVED) {
            throw new AppException(ErrorCode.INVALID_STATE, "승인된 공식 리비전만 릴리즈할 수 있습니다");
        }
        if (part.getCurrentApprovedRevisionId() != null && !part.getCurrentApprovedRevisionId().equals(revision.getId())) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "현재 최신 승인 리비전이 아닙니다. 최신 승인 리비전만 릴리즈할 수 있습니다"
            );
        }
    }

    private void supersedeCurrentApprovedIfNeeded(Part part, UUID nextRevisionId, UUID actorId) {
        UUID currentApprovedRevisionId = part.getCurrentApprovedRevisionId();
        if (currentApprovedRevisionId == null || currentApprovedRevisionId.equals(nextRevisionId)) {
            return;
        }
        if (currentApprovedRevisionId.equals(part.getCurrentReleasedRevisionId())) {
            return;
        }
        getRequiredRevision(currentApprovedRevisionId).markSuperseded(actorId);
    }

    private void supersedeCurrentReleasedIfNeeded(Part part, UUID nextRevisionId, UUID actorId) {
        UUID currentReleasedRevisionId = part.getCurrentReleasedRevisionId();
        if (currentReleasedRevisionId == null || currentReleasedRevisionId.equals(nextRevisionId)) {
            return;
        }
        getRequiredRevision(currentReleasedRevisionId).markSuperseded(actorId);
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
        UUID revisionId = part.getCurrentApprovedRevisionId() != null
                ? part.getCurrentApprovedRevisionId()
                : part.getCurrentReleasedRevisionId();
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
                    PartRevision.CODE_PART_REVISION_IN_REVIEW_REQUIRED,
                    PartRevision.CODE_PART_REVISION_APPROVABLE_REQUIRED,
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
