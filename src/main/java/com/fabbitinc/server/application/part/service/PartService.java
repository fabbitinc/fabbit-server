package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;
    private final PropertyApi propertyApi;
    private final ObjectMapper objectMapper;
    private final PartNumberService partNumberService;
    private final PartCategoryRepository partCategoryRepository;

    public PartRevision createPart(CreatePartInput input, UUID actorId) {
        try {
            String partNumber = input.partNumber();
            if (partNumber != null && partNumber.isBlank()) {
                partNumber = null;
            }
            PartCategory category = partCategoryRepository.findById(input.categoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다: " + input.categoryId()));
            if (category.isAutoNumberingEnabled()) {
                if (partNumber != null) {
                    throw new AppException(ErrorCode.BAD_REQUEST, "자동채번 카테고리에서는 품번을 직접 입력할 수 없습니다");
                }
                partNumber = partNumberService.generate(input.categoryId());
            } else if (partNumber == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "수동 품번 카테고리에서는 품번을 직접 입력해야 합니다");
            }

            Part part = Part.create(partNumber, input.categoryId(), input.itemType());
            if (partRepository.findByPartNumber(part.getPartNumber()).isPresent()) {
                throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 품번입니다: " + part.getPartNumber());
            }
            if (input.lifecycleState() != null) {
                part.forceLifecycleState(input.lifecycleState());
            }

            Part savedPart = partRepository.save(part);
            PartRevision initialRevision = PartRevision.createInitialDraft(savedPart, input.name(), actorId);
            applyCreateInput(initialRevision, input);
            initialRevision.recordHistory(
                    actorId,
                    com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType.CREATED,
                    com.fabbitinc.server.domain.part.model.PartRevisionHistorySourceType.USER,
                    null,
                    serializeReasonPayload(input.reason())
            );
            partRevisionRepository.save(initialRevision);
            return initialRevision;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public List<File> attachFiles(UUID partId, UUID revisionId, List<UUID> fileIds) {
        getRevisionOrThrow(partId, revisionId);

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

        files.forEach(file -> file.assignOwner("part_revision", revisionId));
        long totalBytes = files.stream().mapToLong(File::getFileSize).sum();
        if (totalBytes > 0L) {
            organizationApi.consumeStorageForCurrentTenant(totalBytes);
        }
        return files;
    }

    public void detachFile(UUID partId, UUID revisionId, UUID fileId, UUID actorId) {
        getRevisionOrThrow(partId, revisionId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, "part_revision", revisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '" + revisionId + "'에 연결된 파일 '" + fileId + "'을(를) 찾을 수 없습니다"
                ));
        long fileSize = file.getFileSize();
        file.softDelete(actorId);
        if (fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }
    }


    private PartRevision getRevisionOrThrow(UUID partId, UUID revisionId) {
        return partRevisionRepository.findByIdAndPartId(revisionId, partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partId, revisionId)
                ));
    }

    private void applyCreateInput(PartRevision revision, CreatePartInput input) {
        if (input.material() != null) {
            revision.changeMaterial(input.material());
        }
        if (input.unit() != null) {
            revision.changeUnit(input.unit());
        }
        if (input.description() != null) {
            revision.changeDescription(input.description());
        }
        if (input.leadTimeDays() != null) {
            revision.changeLeadTimeDays(input.leadTimeDays());
        }
        if (!input.extendedProperties().isEmpty()) {
            revision.changeExtendedProperties(
                    serializeProperties(propertyApi.validateExtendedProperties(
                            PropertyOwnerType.PART,
                            input.extendedProperties()
                    ))
            );
        }
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


    private AppException toAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case Part.CODE_PART_NUMBER_REQUIRED,
                    Part.CODE_PART_NUMBER_TOO_LONG,
                    Part.CODE_PART_NUMBER_INVALID_FORMAT,
                    PartRevision.CODE_PART_REVISION_NAME_TOO_LONG,
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
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
