package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PartService {

    private static final int MAX_CATEGORY_LENGTH = 100;

    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;
    private final ObjectMapper objectMapper;

    public PartRevision createPart(CreatePartInput input, UUID actorId) {
        try {
            Part part = Part.create(input.partNumber());
            if (partRepository.findByPartNumber(part.getPartNumber()).isPresent()) {
                throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 품번입니다: " + part.getPartNumber());
            }
            if (input.lifecycleState() != null) {
                part.changeLifecycleState(input.lifecycleState());
            }

            Part savedPart = partRepository.save(part);
            PartRevision initialRevision = PartRevision.createInitialDraft(savedPart, "D1", input.name(), actorId);
            applyCreateInput(initialRevision, input);
            applyDefaultOwner(initialRevision, initialRevision.getCategory());
            initialRevision.recordActivity(
                    actorId,
                    com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType.CREATED,
                    com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType.UI,
                    null,
                    serializeReasonPayload(input.reason())
            );
            partRevisionRepository.save(initialRevision);
            return initialRevision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public PartRevision updateOwner(
            UUID partRevisionId,
            UUID ownerId,
            boolean ownerIdSet,
            UUID ownerTeamId,
            boolean ownerTeamIdSet
    ) {
        PartRevision revision = getRevisionOrThrow(partRevisionId);

        if (ownerIdSet) {
            if (ownerId == null) {
                revision.unassignOwner();
            } else {
                revision.assignOwner(ownerId);
            }
        }

        if (ownerTeamIdSet) {
            if (ownerTeamId == null) {
                revision.unassignOwnerTeam();
            } else {
                revision.assignOwnerTeam(ownerTeamId);
            }
        }
        return revision;
    }

    public List<File> attachFiles(UUID partRevisionId, List<UUID> fileIds) {
        getRevisionOrThrow(partRevisionId);

        List<File> files = fileRepository.findByIdIn(fileIds);
        Set<UUID> foundIds = files.stream().map(File::getId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> missingIds = new LinkedHashSet<>(fileIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다: " + missingIds);
        }

        List<UUID> notUploaded = files.stream()
                .filter(file -> file.getStatus() != FileStatus.UPLOADED)
                .map(File::getId)
                .toList();
        if (!notUploaded.isEmpty()) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "업로드 완료되지 않은 파일이 있습니다: " + notUploaded
            );
        }

        List<UUID> alreadyOwned = files.stream()
                .filter(file -> file.getOwnerId() != null)
                .map(File::getId)
                .toList();
        if (!alreadyOwned.isEmpty()) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 다른 리소스에 연결된 파일이 있습니다: " + alreadyOwned
            );
        }

        files.forEach(file -> file.assignOwner("part_revision", partRevisionId));
        long totalBytes = files.stream().mapToLong(File::getFileSize).sum();
        if (totalBytes > 0L) {
            organizationApi.consumeStorageForCurrentTenant(totalBytes);
        }
        return files;
    }

    public void detachFile(UUID partRevisionId, UUID fileId, UUID actorId) {
        getRevisionOrThrow(partRevisionId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, "part_revision", partRevisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '" + partRevisionId + "'에 연결된 파일 '" + fileId + "'을(를) 찾을 수 없습니다"
                ));
        long fileSize = file.getFileSize();
        file.softDelete(actorId);
        if (fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }
    }

    public PartDefaultOwner upsertDefaultOwner(String category, UUID ownerId, UUID ownerTeamId) {
        if (ownerId == null && ownerTeamId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "기본 담당자 또는 기본 담당 팀 중 하나는 필수입니다");
        }

        PartDefaultOwner defaultOwner;
        if (category != null) {
            defaultOwner = partDefaultOwnerRepository.findByCategory(category)
                    .orElseGet(() -> PartDefaultOwner.create(category, ownerId, ownerTeamId));
        } else {
            defaultOwner = partDefaultOwnerRepository.findByCategoryIsNull()
                    .orElseGet(() -> PartDefaultOwner.create(null, ownerId, ownerTeamId));
        }

        defaultOwner.update(ownerId, ownerTeamId);
        return partDefaultOwnerRepository.save(defaultOwner);
    }

    public void deleteDefaultOwner(String category) {
        long deleted = category != null
                ? partDefaultOwnerRepository.deleteByCategory(category)
                : partDefaultOwnerRepository.deleteByCategoryIsNull();

        if (deleted == 0) {
            String categoryText = category == null ? "None" : category;
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "카테고리 '" + categoryText + "' 기본값 설정을 찾을 수 없습니다"
            );
        }
    }

    public int renameCategory(String oldName, String newName) {
        String normalizedOldName = normalizeRequiredCategory(oldName, "기존");
        String normalizedNewName = normalizeRequiredCategory(newName, "변경");

        if (normalizedOldName.equals(normalizedNewName)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 전후 카테고리 이름이 동일합니다");
        }

        boolean hasOldCategory = partRevisionRepository.existsByCategory(normalizedOldName);
        if (!hasOldCategory) {
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "카테고리 '" + normalizedOldName + "'을(를) 찾을 수 없습니다"
            );
        }

        boolean isMerge = partRevisionRepository.existsByCategory(normalizedNewName);
        int updatedCount = partRevisionRepository.renameCategory(normalizedOldName, normalizedNewName);

        if (isMerge) {
            partDefaultOwnerRepository.deleteByCategory(normalizedOldName);
        } else {
            partDefaultOwnerRepository.renameCategory(normalizedOldName, normalizedNewName);
        }
        return updatedCount;
    }

    private PartRevision getRevisionOrThrow(UUID partRevisionId) {
        return partRevisionRepository.findById(partRevisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '" + partRevisionId + "'을(를) 찾을 수 없습니다"
                ));
    }

    private void applyCreateInput(PartRevision revision, CreatePartInput input) {
        if (input.category() != null) {
            revision.changeCategory(input.category());
        }
        if (input.material() != null) {
            revision.changeMaterial(input.material());
        }
        if (input.unit() != null) {
            revision.changeUnit(input.unit());
        }
        if (input.description() != null) {
            revision.changeDescription(input.description());
        }
        if (input.phantom() != null) {
            applyPhantom(revision, input.phantom());
        }
        if (input.leadTimeDays() != null) {
            revision.changeLeadTimeDays(input.leadTimeDays());
        }
        if (!input.extendedProperties().isEmpty()) {
            revision.changeExtendedProperties(serializeProperties(input.extendedProperties()));
        }
    }

    private void applyDefaultOwner(PartRevision revision, String category) {
        resolveDefaultOwner(category).ifPresent(defaultOwner -> {
            if (defaultOwner.getDefaultOwnerId() != null) {
                revision.assignOwner(defaultOwner.getDefaultOwnerId());
            }
            if (defaultOwner.getDefaultOwnerTeamId() != null) {
                revision.assignOwnerTeam(defaultOwner.getDefaultOwnerTeamId());
            }
        });
    }

    private void applyPhantom(PartRevision revision, Boolean phantom) {
        if (Boolean.TRUE.equals(phantom)) {
            revision.markPhantom();
            return;
        }
        revision.markReal();
    }

    private Optional<PartDefaultOwner> resolveDefaultOwner(String category) {
        if (category != null) {
            Optional<PartDefaultOwner> categoryDefaultOwner = partDefaultOwnerRepository.findByCategory(category);
            if (categoryDefaultOwner.isPresent()) {
                return categoryDefaultOwner;
            }
        }
        return partDefaultOwnerRepository.findByCategoryIsNull();
    }

    private String serializeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "extended_properties를 직렬화할 수 없습니다");
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

    private String normalizeRequiredCategory(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, label + " 카테고리는 비어 있을 수 없습니다");
        }

        String trimmed = raw.trim();
        if (trimmed.length() > MAX_CATEGORY_LENGTH) {
            throw new AppException(ErrorCode.BAD_REQUEST, label + " 카테고리는 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private AppException toAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case Part.CODE_PART_NUMBER_REQUIRED,
                    Part.CODE_PART_NUMBER_TOO_LONG,
                    Part.CODE_PART_NUMBER_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_NAME_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_CATEGORY_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_MATERIAL_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_UNIT_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_REQUIRED,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_PART_NUMBER_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_CODE_REQUIRED,
                    PartRevision.CODE_PART_REVISION_CODE_TOO_LONG,
                    PartRevision.CODE_PART_REVISION_CODE_INVALID_FORMAT,
                    Part.CODE_PART_OWNER_REQUIRED,
                    Part.CODE_PART_OWNER_TEAM_REQUIRED,
                    PartRevision.CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
