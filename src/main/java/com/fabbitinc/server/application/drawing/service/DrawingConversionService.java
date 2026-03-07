package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DrawingConversionService {

    private static final Set<String> CAD_EXTENSIONS = Set.of(".dwg", ".dxf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff");
    private static final float THUMBNAIL_DPI = 150.0f;

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final OrganizationApi organizationApi;
    private final DrawingConverterProperties drawingConverterProperties;
    private final Semaphore semaphore;

    public DrawingConversionService(
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            StoragePort storagePort,
            OrganizationApi organizationApi,
            DrawingConverterProperties drawingConverterProperties
    ) {
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.storagePort = storagePort;
        this.organizationApi = organizationApi;
        this.drawingConverterProperties = drawingConverterProperties;
        this.semaphore = new Semaphore(Math.max(1, drawingConverterProperties.maxConcurrent()));
    }

    public void convertDrawing(UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null) {
            log.warn("event=drawing_conversion_skipped drawing_id={} reason=drawing_not_found", drawingId);
            return;
        }
        if (drawing.getDeletedAt() != null) {
            log.warn("event=drawing_conversion_skipped drawing_id={} reason=drawing_deleted", drawingId);
            return;
        }

        File originalFile = fileRepository.findByFileKeyAndDeletedAtIsNull(drawing.getOriginalFileKey()).orElse(null);
        if (originalFile == null) {
            markConversionFailed(drawingId, "original_file_not_found");
            return;
        }

        try {
            ConversionArtifacts artifacts = runConversion(drawingId, originalFile);
            applyConversionCompleted(drawingId, originalFile, artifacts);
        } catch (Exception ex) {
            log.error(
                    "event=drawing_conversion_failed drawing_id={} file_key={} reason={}",
                    drawingId,
                    originalFile.getFileKey(),
                    ex.getMessage(),
                    ex
            );
            markConversionFailed(drawingId, ex.getMessage());
        }
    }

    private ConversionArtifacts runConversion(UUID drawingId, File originalFile) throws Exception {
        semaphore.acquire();

        Path workDir = createWorkDirectory(drawingId);
        List<String> uploadedKeys = new ArrayList<>();
        try {
            String extension = extractExtension(originalFile.getOriginalName());
            Path inputPath = workDir.resolve(originalFile.getOriginalName());
            Files.createDirectories(inputPath.getParent());
            Files.write(inputPath, storagePort.getObject(originalFile.getFileKey()));

            String pdfKey = replaceSuffix(originalFile.getFileKey(), ".pdf");
            String thumbnailKey = buildThumbnailKey(originalFile.getFileKey());
            Path pdfPath = workDir.resolve(replaceSuffix(originalFile.getOriginalName(), ".pdf"));
            Path thumbnailPath = workDir.resolve(buildThumbnailName(originalFile.getOriginalName()));

            boolean reuseOriginalPdf = ".pdf".equals(extension);
            long pdfSize = originalFile.getFileSize();

            if (CAD_EXTENSIONS.contains(extension)) {
                generatePdfFromCad(inputPath, pdfPath);
                byte[] pdfBytes = Files.readAllBytes(pdfPath);
                storagePort.putObject(pdfKey, pdfBytes, "application/pdf");
                uploadedKeys.add(pdfKey);
                pdfSize = pdfBytes.length;
            } else if (IMAGE_EXTENSIONS.contains(extension)) {
                generatePdfFromImage(inputPath, pdfPath);
                byte[] pdfBytes = Files.readAllBytes(pdfPath);
                storagePort.putObject(pdfKey, pdfBytes, "application/pdf");
                uploadedKeys.add(pdfKey);
                pdfSize = pdfBytes.length;
            } else if (!reuseOriginalPdf) {
                throw new IllegalStateException("지원하지 않는 도면 파일 형식입니다: " + extension);
            }

            Path pdfSourcePath = reuseOriginalPdf ? inputPath : pdfPath;
            byte[] thumbnailBytes = generateThumbnailFromPdf(pdfSourcePath);
            storagePort.putObject(thumbnailKey, thumbnailBytes, "image/png");
            uploadedKeys.add(thumbnailKey);

            return new ConversionArtifacts(
                    reuseOriginalPdf ? originalFile.getFileKey() : pdfKey,
                    "application/pdf",
                    pdfSize,
                    thumbnailKey,
                    "image/png",
                    thumbnailBytes.length,
                    reuseOriginalPdf
            );
        } catch (Exception ex) {
            cleanupUploadedObjects(uploadedKeys);
            throw ex;
        } finally {
            cleanupWorkDirectory(workDir);
            semaphore.release();
        }
    }

    private void applyConversionCompleted(UUID drawingId, File originalFile, ConversionArtifacts artifacts) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            cleanupUploadedObjects(List.of(
                    artifacts.pdfReusedOriginal() ? null : artifacts.pdfKey(),
                    artifacts.thumbnailKey()
            ));
            log.warn("event=drawing_conversion_skipped drawing_id={} reason=drawing_missing_on_apply", drawingId);
            return;
        }

        if (!artifacts.pdfReusedOriginal()) {
            upsertGeneratedFile(
                    drawing.getId(),
                    artifacts.pdfKey(),
                    replaceSuffix(originalFile.getOriginalName(), ".pdf"),
                    artifacts.pdfContentType(),
                    artifacts.pdfSize()
            );
        }
        upsertGeneratedFile(
                drawing.getId(),
                artifacts.thumbnailKey(),
                replaceSuffix(originalFile.getOriginalName(), "_thumbnail.png"),
                artifacts.thumbnailContentType(),
                artifacts.thumbnailSize()
        );

        drawing.markConversionCompleted(artifacts.pdfKey(), artifacts.thumbnailKey());
        drawingRepository.save(drawing);

        log.info(
                "event=drawing_conversion_completed drawing_id={} pdf_key={} thumbnail_key={} outcome=success",
                drawingId,
                artifacts.pdfKey(),
                artifacts.thumbnailKey()
        );
    }

    private void markConversionFailed(UUID drawingId, String reason) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            return;
        }

        drawing.markConversionFailed();
        drawingRepository.save(drawing);
        log.warn("event=drawing_conversion_marked_failed drawing_id={} reason={}", drawingId, reason);
    }

    private File upsertGeneratedFile(UUID drawingId, String fileKey, String originalName, String contentType, long fileSize) {
        File generatedFile = fileRepository.findByFileKeyAndDeletedAtIsNull(fileKey)
                .orElseGet(() -> File.create(UuidV7Generator.next(), originalName, fileKey, contentType, fileSize));
        boolean consumedStorage = false;

        if (generatedFile.getStatus() != com.fabbitinc.server.domain.file.model.FileStatus.UPLOADED) {
            generatedFile.markUploaded();
        }
        if (generatedFile.getOwnerId() == null) {
            generatedFile.assignOwner("drawing", drawingId);
            if (generatedFile.getFileSize() > 0L) {
                organizationApi.consumeStorageForCurrentTenant(generatedFile.getFileSize());
                consumedStorage = true;
            }
        }
        try {
            return fileRepository.save(generatedFile);
        } catch (RuntimeException ex) {
            if (consumedStorage) {
                organizationApi.releaseStorageForCurrentTenant(generatedFile.getFileSize());
            }
            throw ex;
        }
    }

    private Path createWorkDirectory(UUID drawingId) throws IOException {
        Path baseDir = Paths.get(drawingConverterProperties.tempDir());
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, "drawing-" + drawingId + "-");
    }

    private void cleanupWorkDirectory(Path workDir) {
        if (workDir == null) {
            return;
        }
        try (var walk = Files.walk(workDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private void cleanupUploadedObjects(List<String> fileKeys) {
        fileKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .forEach(key -> {
                    try {
                        storagePort.deleteObject(key);
                    } catch (RuntimeException ex) {
                        log.warn("event=drawing_conversion_uploaded_cleanup_failed file_key={} reason={}", key, ex.getMessage());
                    }
                });
    }

    private void generatePdfFromCad(Path inputPath, Path outputPath) throws Exception {
        Path executable = Paths.get(drawingConverterProperties.qcadPath(), "dwg2pdf");
        if (!Files.exists(executable)) {
            throw new IllegalStateException("QCAD dwg2pdf 실행 파일을 찾을 수 없습니다: " + executable);
        }

        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        if (!isMac()) {
            command.add("-platform");
            command.add("offscreen");
        }
        command.add("-f");
        command.add("-auto-fit");
        command.add("-auto-orientation");
        command.add("-o");
        command.add(outputPath.toString());
        command.add(inputPath.toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(300, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("QCAD dwg2pdf 실행 시간이 초과되었습니다");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("QCAD dwg2pdf 실패: " + output);
        }
        if (!Files.exists(outputPath)) {
            throw new IllegalStateException("QCAD dwg2pdf 결과 PDF가 생성되지 않았습니다");
        }
    }

    private void generatePdfFromImage(Path inputPath, Path outputPath) throws IOException {
        BufferedImage image = ImageIO.read(inputPath.toFile());
        if (image == null) {
            throw new IOException("이미지 파일을 읽을 수 없습니다: " + inputPath.getFileName());
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
            }

            document.save(outputPath.toFile());
        }
    }

    private byte[] generateThumbnailFromPdf(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, THUMBNAIL_DPI);
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx).toLowerCase(Locale.ROOT);
    }

    private String replaceSuffix(String value, String suffix) {
        int idx = value.lastIndexOf('.');
        if (idx < 0) {
            return value + suffix;
        }
        return value.substring(0, idx) + suffix;
    }

    private String buildThumbnailKey(String originalFileKey) {
        String thumbnailKey = replaceSuffix(originalFileKey, ".png");
        if (thumbnailKey.equals(originalFileKey)) {
            return replaceSuffix(originalFileKey, "_thumbnail.png");
        }
        return thumbnailKey;
    }

    private String buildThumbnailName(String originalName) {
        String thumbnailName = replaceSuffix(originalName, ".png");
        if (thumbnailName.equals(originalName)) {
            return replaceSuffix(originalName, "_thumbnail.png");
        }
        return thumbnailName;
    }

    private record ConversionArtifacts(
            String pdfKey,
            String pdfContentType,
            long pdfSize,
            String thumbnailKey,
            String thumbnailContentType,
            long thumbnailSize,
            boolean pdfReusedOriginal
    ) {
    }
}
