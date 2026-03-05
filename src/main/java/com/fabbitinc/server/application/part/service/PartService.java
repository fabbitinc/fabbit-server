package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;
    private final PartDefaultOwnerRepository partDefaultOwnerRepository;
    private final FileRepository fileRepository;

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
        return files;
    }

    public void detachFile(UUID partId, UUID fileId) {
        getPartOrThrow(partId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, "part", partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'에 연결된 파일 '" + fileId + "'을(를) 찾을 수 없습니다"
                ));
        file.softDelete();
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
        if (oldName.equals(newName)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "변경 전후 카테고리 이름이 동일합니다");
        }

        boolean hasOldCategory = partRepository.existsByCategory(oldName);
        if (!hasOldCategory) {
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "카테고리 '" + oldName + "'을(를) 찾을 수 없습니다"
            );
        }

        boolean isMerge = partRepository.existsByCategory(newName);
        int updatedCount = partRepository.renameCategory(oldName, newName);

        if (isMerge) {
            partDefaultOwnerRepository.deleteByCategory(oldName);
        } else {
            partDefaultOwnerRepository.renameCategory(oldName, newName);
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
}
