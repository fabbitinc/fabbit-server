package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.dto.request.BatchCompleteRequest;
import com.fabbitinc.server.application.file.dto.request.BatchCreateFileRequest;
import com.fabbitinc.server.application.file.dto.request.CreateFileRequest;
import com.fabbitinc.server.application.file.dto.response.BatchCompleteFailure;
import com.fabbitinc.server.application.file.dto.response.BatchCompleteResponse;
import com.fabbitinc.server.application.file.dto.response.BatchCreateFileResponse;
import com.fabbitinc.server.application.file.dto.response.CreateFileResponse;
import com.fabbitinc.server.application.file.dto.response.FileCompleteResponse;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final StoragePort storagePort;

    public CreateFileResponse createFile(AuthContext auth, CreateFileRequest request) {
        log.info("auth: " + auth + " request: " + request + "");
        UUID fileId = UuidV7Generator.next();
        String fileKey = "tenants/" + auth.orgId() + "/uploaded/" + fileId + "/" + request.originalName();

        File file = fileRepository.save(
                new File(fileId, request.originalName(), fileKey, request.contentType(), request.fileSize())
        );

        String uploadUrl = storagePort.generateUploadPresignedUrl(
                file.getFileKey(),
                file.getContentType(),
                file.getFileSize()
        );
        return new CreateFileResponse(file.getId(), uploadUrl, file.getFileKey());
    }

    public BatchCreateFileResponse batchCreateFiles(AuthContext auth, BatchCreateFileRequest request) {
        List<CreateFileResponse> items = new ArrayList<>(request.items().size());
        for (CreateFileRequest item : request.items()) {
            UUID fileId = UuidV7Generator.next();
            String fileKey = "tenants/" + auth.orgId() + "/raw_data/" + fileId + "/" + item.originalName();

            File file = fileRepository.save(
                    new File(fileId, item.originalName(), fileKey, item.contentType(), item.fileSize())
            );

            String uploadUrl = storagePort.generateUploadPresignedUrl(
                    file.getFileKey(),
                    file.getContentType(),
                    file.getFileSize()
            );
            items.add(new CreateFileResponse(file.getId(), uploadUrl, file.getFileKey()));
        }
        return new BatchCreateFileResponse(items);
    }

    public BatchCompleteResponse batchCompleteFiles(BatchCompleteRequest request) {
        List<File> files = fileRepository.findByIdIn(request.fileIds());
        Map<UUID, File> fileMap = new HashMap<>();
        for (File file : files) {
            fileMap.put(file.getId(), file);
        }

        List<FileCompleteResponse> completed = new ArrayList<>();
        List<BatchCompleteFailure> failed = new ArrayList<>();

        for (UUID fileId : request.fileIds()) {
            File file = fileMap.get(fileId);
            if (file == null) {
                failed.add(new BatchCompleteFailure(fileId, "파일을 찾을 수 없습니다"));
                continue;
            }

            if (file.getStatus() == FileStatus.UPLOADED) {
                failed.add(new BatchCompleteFailure(fileId, "이미 완료된 업로드입니다"));
                continue;
            }

            if (storagePort.headObject(file.getFileKey()) == null) {
                failed.add(new BatchCompleteFailure(fileId, "S3에 파일이 존재하지 않습니다"));
                continue;
            }

            file.markUploaded();
            completed.add(toCompleteResponse(file));
        }

        return new BatchCompleteResponse(completed, failed);
    }

    public FileCompleteResponse completeFile(UUID fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));

        if (file.getStatus() == FileStatus.UPLOADED) {
            throw new AppException(ErrorCode.CONFLICT, "이미 완료된 업로드입니다");
        }

        if (storagePort.headObject(file.getFileKey()) == null) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "S3에 파일이 존재하지 않습니다. 업로드를 완료해주세요."
            );
        }

        file.markUploaded();
        return toCompleteResponse(file);
    }

    public List<File> validateAttachable(List<UUID> fileIds) {
        List<File> files = fileRepository.findByIdIn(fileIds);

        Set<UUID> foundIds = files.stream().map(File::getId).collect(Collectors.toSet());
        List<UUID> missingIds = fileIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다: " + missingIds);
        }

        List<UUID> notUploadedIds = files.stream()
                .filter(file -> file.getStatus() != FileStatus.UPLOADED)
                .map(File::getId)
                .toList();
        if (!notUploadedIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_STATE, "업로드 완료되지 않은 파일이 있습니다: " + notUploadedIds);
        }

        List<UUID> alreadyOwnedIds = files.stream()
                .filter(file -> file.getOwnerId() != null)
                .map(File::getId)
                .toList();
        if (!alreadyOwnedIds.isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "이미 다른 리소스에 연결된 파일이 있습니다: " + alreadyOwnedIds);
        }

        return files.stream().sorted(Comparator.comparing(File::getId)).toList();
    }

    public void convertToThumbnail(File file) {
        file.changeToThumbnailWebp();
    }

    public List<File> getFilesByOwner(String ownerType, UUID ownerId) {
        return fileRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(ownerType, ownerId);
    }

    public void softDelete(UUID fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));
        file.softDelete();
    }

    private FileCompleteResponse toCompleteResponse(File file) {
        return new FileCompleteResponse(
                file.getId(),
                file.getStatus().name(),
                file.getOriginalName(),
                file.getFileKey(),
                file.getFileSize(),
                file.getContentType(),
                file.getCreatedAt()
        );
    }
}
