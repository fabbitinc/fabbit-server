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
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
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
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;
    private final ObjectMapper objectMapper;

    public Part createPart(CreatePartInput input) {
        try {
            Part part = Part.create(input.partNumber(), input.name());
            if (partRepository.findByPartNumber(part.getPartNumber()).isPresent()) {
                throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 품번입니다: " + part.getPartNumber());
            }

            applyCreateInput(part, input);
            applyDefaultOwner(part);
            return partRepository.save(part);
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public Part updateOwner(
            UUID partId,
            UUID ownerId,
            boolean ownerIdSet,
            UUID ownerTeamId,
            boolean ownerTeamIdSet
    ) {
        Part part = getPartOrThrow(partId);

        if (ownerIdSet) {
            if (ownerId == null) {
                part.unassignOwner();
            } else {
                part.assignOwner(ownerId);
            }
        }

        if (ownerTeamIdSet) {
            if (ownerTeamId == null) {
                part.unassignOwnerTeam();
            } else {
                part.assignOwnerTeam(ownerTeamId);
            }
        }
        return part;
    }

    public List<File> attachFiles(UUID partId, List<UUID> fileIds) {
        getPartOrThrow(partId);

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

        files.forEach(file -> file.assignOwner("part", partId));
        long totalBytes = files.stream().mapToLong(File::getFileSize).sum();
        if (totalBytes > 0L) {
            organizationApi.consumeStorageForCurrentTenant(totalBytes);
        }
        return files;
    }

    public void detachFile(UUID partId, UUID fileId) {
        getPartOrThrow(partId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, "part", partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'에 연결된 파일 '" + fileId + "'을(를) 찾을 수 없습니다"
                ));
        long fileSize = file.getFileSize();
        file.softDelete();
        if (fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }
    }

    public void assignDrawing(UUID partId, UUID drawingId) {
        Part part = getPartOrThrow(partId);
        if (part.getDrawingId() != null) {
            part.unassignDrawing();
        }
        part.assignDrawing(drawingId);
    }

    public UUID unassignDrawing(UUID partId) {
        Part part = getPartOrThrow(partId);
        if (part.getDrawingId() == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "연결된 도면이 없습니다");
        }

        UUID drawingId = part.getDrawingId();
        part.unassignDrawing();
        return drawingId;
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

        boolean hasOldCategory = partRepository.existsByCategory(normalizedOldName);
        if (!hasOldCategory) {
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "카테고리 '" + normalizedOldName + "'을(를) 찾을 수 없습니다"
            );
        }

        boolean isMerge = partRepository.existsByCategory(normalizedNewName);
        int updatedCount = partRepository.renameCategory(normalizedOldName, normalizedNewName);

        if (isMerge) {
            partDefaultOwnerRepository.deleteByCategory(normalizedOldName);
        } else {
            partDefaultOwnerRepository.renameCategory(normalizedOldName, normalizedNewName);
        }
        return updatedCount;
    }

    private Part getPartOrThrow(UUID partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));
    }

    private void applyCreateInput(Part part, CreatePartInput input) {
        if (input.category() != null) {
            part.changeCategory(input.category());
        }
        if (input.material() != null) {
            part.changeMaterial(input.material());
        }
        if (input.unit() != null) {
            part.changeUnit(input.unit());
        }
        if (input.description() != null) {
            part.changeDescription(input.description());
        }
        if (input.phantom() != null) {
            applyPhantom(part, input.phantom());
        }

        PartLifecycleState lifecycleState = parseLifecycleState(input.lifecycleState());
        if (lifecycleState != null) {
            part.changeLifecycleState(lifecycleState);
        }
        if (input.leadTimeDays() != null) {
            part.changeLeadTimeDays(input.leadTimeDays());
        }
        if (!input.extendedProperties().isEmpty()) {
            part.changeExtendedProperties(serializeProperties(input.extendedProperties()));
        }
    }

    private void applyPhantom(Part part, Boolean phantom) {
        if (Boolean.TRUE.equals(phantom)) {
            part.markPhantom();
            return;
        }
        part.markReal();
    }

    private PartLifecycleState parseLifecycleState(String rawLifecycleState) {
        if (rawLifecycleState == null || rawLifecycleState.isBlank()) {
            return null;
        }

        PartLifecycleState lifecycleState = PartLifecycleState.from(rawLifecycleState);
        if (lifecycleState == null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "유효하지 않은 lifecycle_state입니다: " + rawLifecycleState
            );
        }
        return lifecycleState;
    }

    private void applyDefaultOwner(Part part) {
        resolveDefaultOwner(part.getCategory()).ifPresent(defaultOwner -> {
            if (defaultOwner.getDefaultOwnerId() != null) {
                part.assignOwner(defaultOwner.getDefaultOwnerId());
            }
            if (defaultOwner.getDefaultOwnerTeamId() != null) {
                part.assignOwnerTeam(defaultOwner.getDefaultOwnerTeamId());
            }
        });
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
                    Part.CODE_PART_NAME_TOO_LONG,
                    Part.CODE_PART_CATEGORY_TOO_LONG,
                    Part.CODE_PART_MATERIAL_TOO_LONG,
                    Part.CODE_PART_UNIT_TOO_LONG,
                    Part.CODE_PART_DRAWING_REQUIRED,
                    Part.CODE_PART_OWNER_REQUIRED,
                    Part.CODE_PART_OWNER_TEAM_REQUIRED,
                    Part.CODE_PART_LEAD_TIME_DAYS_INVALID ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
