package com.fabbitinc.server.application.file.service;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.file.service.input.CreateFileInput;
import com.fabbitinc.server.application.file.service.output.BatchCompleteFailureOutput;
import com.fabbitinc.server.application.file.service.output.BatchCompleteFilesOutput;
import com.fabbitinc.server.application.file.service.output.BatchCreateFilesOutput;
import com.fabbitinc.server.application.file.service.output.CreateFileOutput;
import com.fabbitinc.server.application.file.service.output.FileCompleteOutput;
import com.fabbitinc.server.application.image.support.ImageVariantProcessor;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileService {

    private static final int PROFILE_THUMBNAIL_SIZE = 256;
    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private static final String UPLOADED_DIRECTORY = "uploaded";
    private static final String RAW_DATA_DIRECTORY = "raw_data";

    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final OrganizationApi organizationApi;
    private final ImageVariantProcessor imageVariantService;

    public FileService(
            FileRepository fileRepository,
            StoragePort storagePort,
            OrganizationApi organizationApi
    ) {
        this.fileRepository = fileRepository;
        this.storagePort = storagePort;
        this.organizationApi = organizationApi;
        this.imageVariantService = new ImageVariantProcessor(storagePort);
    }

    public CreateFileOutput createFile(AuthContext auth, CreateFileInput input) {
        return createFile(auth, input, UPLOADED_DIRECTORY);
    }

    public BatchCreateFilesOutput batchCreateFiles(AuthContext auth, List<CreateFileInput> inputs) {
        return batchCreateFiles(auth, inputs, UPLOADED_DIRECTORY);
    }

    public BatchCreateFilesOutput batchCreateRawFiles(AuthContext auth, List<CreateFileInput> inputs) {
        return batchCreateFiles(auth, inputs, RAW_DATA_DIRECTORY);
    }

    private BatchCreateFilesOutput batchCreateFiles(AuthContext auth, List<CreateFileInput> inputs, String directory) {
        List<CreateFileOutput> items = new ArrayList<>(inputs.size());
        for (CreateFileInput input : inputs) {
            items.add(createFile(auth, input, directory));
        }
        return new BatchCreateFilesOutput(items);
    }

    private CreateFileOutput createFile(AuthContext auth, CreateFileInput input, String directory) {
        UUID fileId = UuidV7Generator.next();
        String fileKey = "tenants/" + auth.orgId() + "/" + directory + "/" + fileId + "/" + input.originalName();

        File file = fileRepository.save(
                File.create(
                        fileId,
                        input.originalName(),
                        fileKey,
                        input.contentType(),
                        input.fileSize(),
                        input.contentHash()
                )
        );

        String uploadUrl = storagePort.generateUploadPresignedUrl(
                file.getFileKey(),
                file.getContentType()
        );
        return new CreateFileOutput(file.getId(), uploadUrl, file.getFileKey());
    }

    public BatchCompleteFilesOutput completeFiles(List<UUID> fileIds) {
        List<File> files = fileRepository.findByIdIn(fileIds);
        Map<UUID, File> fileMap = new HashMap<>();
        for (File file : files) {
            fileMap.put(file.getId(), file);
        }

        List<FileCompleteOutput> completed = new ArrayList<>();
        List<BatchCompleteFailureOutput> failed = new ArrayList<>();

        for (UUID fileId : fileIds) {
            File file = fileMap.get(fileId);
            if (file == null) {
                failed.add(new BatchCompleteFailureOutput(fileId, "파일을 찾을 수 없습니다"));
                continue;
            }

            if (file.getStatus() == FileStatus.UPLOADED) {
                failed.add(new BatchCompleteFailureOutput(fileId, "이미 완료된 업로드입니다"));
                continue;
            }

            if (storagePort.headObject(file.getFileKey()) == null) {
                failed.add(new BatchCompleteFailureOutput(fileId, "S3에 파일이 존재하지 않습니다"));
                continue;
            }

            file.markUploaded();
            completed.add(toCompleteResponse(file));
        }

        return new BatchCompleteFilesOutput(completed, failed);
    }

    public FileCompleteOutput completeFile(UUID fileId) {
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
        imageVariantService.convertToThumbnail(file);
    }

    public List<File> getFilesByOwner(String ownerType, UUID ownerId) {
        return fileRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(ownerType, ownerId);
    }

    public void softDelete(UUID fileId, UUID actorId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));
        boolean releaseStorage = file.getOwnerId() != null && file.getStatus() == FileStatus.UPLOADED;
        long fileSize = file.getFileSize();
        file.softDelete(actorId);
        if (releaseStorage && fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }
    }

    private FileCompleteOutput toCompleteResponse(File file) {
        return new FileCompleteOutput(
                file.getId(),
                file.getStatus(),
                file.getOriginalName(),
                file.getFileKey(),
                file.getFileSize(),
                file.getContentType(),
                file.getCreatedAt()
        );
    }

    private byte[] createThumbnailWebp(byte[] originalBytes) {
        BufferedImage sourceImage = readImage(originalBytes);
        BufferedImage croppedImage = centerCropSquare(sourceImage);
        BufferedImage thumbnailImage = resizeToThumbnail(croppedImage);
        return writeWebp(thumbnailImage);
    }

    private BufferedImage readImage(byte[] originalBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 이미지 형식입니다");
            }
            return image;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 변환 중 오류가 발생했습니다");
        }
    }

    private BufferedImage centerCropSquare(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int cropSize = Math.min(width, height);
        int startX = (width - cropSize) / 2;
        int startY = (height - cropSize) / 2;
        BufferedImage croppedImage = sourceImage.getSubimage(startX, startY, cropSize, cropSize);

        BufferedImage rgbImage = new BufferedImage(cropSize, cropSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(croppedImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    private BufferedImage resizeToThumbnail(BufferedImage sourceImage) {
        BufferedImage thumbnailImage = new BufferedImage(
                PROFILE_THUMBNAIL_SIZE,
                PROFILE_THUMBNAIL_SIZE,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = thumbnailImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, PROFILE_THUMBNAIL_SIZE, PROFILE_THUMBNAIL_SIZE, null);
        } finally {
            graphics.dispose();
        }
        return thumbnailImage;
    }

    private byte[] writeWebp(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, "webp", outputStream);
            if (!written) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "WebP 변환기를 찾을 수 없습니다");
            }
            return outputStream.toByteArray();
        } catch (Exception ex) {
            if (ex instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "WebP 이미지 저장 중 오류가 발생했습니다");
        }
    }

    private String replaceSuffix(String fileKey, String newSuffix) {
        int extensionIndex = fileKey.lastIndexOf('.');
        if (extensionIndex < 0) {
            return fileKey + newSuffix;
        }
        return fileKey.substring(0, extensionIndex) + newSuffix;
    }
}
