package com.fabbitinc.server.application.drawing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import com.fabbitinc.server.domain.drawing.repository.DrawingProcessingJobRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.infrastructure.drawing.adapter.PdfBoxPdfPreviewRenderAdapter;
import com.fabbitinc.server.infrastructure.drawing.adapter.PdfBoxRasterImageToPdfAdapter;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Pdf2dDrawingPipeline;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Raster2dDrawingPipeline;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class DrawingConversionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void convertDrawing_pdf원본은_pdf를_재사용하고_썸네일을_생성한다() throws Exception {
        DrawingRepository drawingRepository = mock(DrawingRepository.class);
        DrawingProcessingJobRepository drawingProcessingJobRepository = mock(DrawingProcessingJobRepository.class);
        DrawingServingProjectionService drawingServingProjectionService = mock(DrawingServingProjectionService.class);
        FileRepository fileRepository = mock(FileRepository.class);
        StoragePort storagePort = mock(StoragePort.class);
        OrganizationApi organizationApi = mock(OrganizationApi.class);
        AtomicReference<DrawingProcessingJob> savedJob = new AtomicReference<>();

        String originalKey = "tenants/org/uploaded/drawing/sample.pdf";

        Drawing drawing = Drawing.create(null, "sample.pdf");
        UUID drawingId = drawing.getId();
        drawing.changeOriginalFileKey(originalKey);
        drawing.markConversionPending();

        byte[] pdfBytes = samplePdf();
        File originalFile = File.create(UUID.randomUUID(), "sample.pdf", originalKey, "application/pdf", pdfBytes.length);
        originalFile.markUploaded();
        originalFile.assignOwner("drawing", drawingId);

        when(drawingRepository.findById(drawingId)).thenReturn(Optional.of(drawing));
        when(fileRepository.findByFileKeyAndDeletedAtIsNull(originalKey)).thenReturn(Optional.of(originalFile));
        when(fileRepository.findByFileKeyAndDeletedAtIsNull("tenants/org/uploaded/drawing/sample.webp"))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingRepository.save(any(Drawing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingProcessingJobRepository.save(any(DrawingProcessingJob.class))).thenAnswer(invocation -> {
            DrawingProcessingJob job = invocation.getArgument(0);
            savedJob.set(job);
            return job;
        });
        when(drawingProcessingJobRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedJob.get()));
        when(drawingProcessingJobRepository.findByIdForUpdate(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedJob.get()));
        when(storagePort.getObject(originalKey)).thenReturn(pdfBytes);
        doNothing().when(storagePort).putObject(eq("tenants/org/uploaded/drawing/sample.webp"), any(byte[].class), eq("image/webp"));

        DrawingConversionService service = createService(
                drawingRepository,
                drawingProcessingJobRepository,
                drawingServingProjectionService,
                fileRepository,
                storagePort,
                organizationApi
        );

        service.requestAndConvertDrawing(drawingId);

        assertEquals(DrawingConversionStatus.COMPLETED, drawing.getConversionStatus());
        assertEquals(DrawingJobStatus.COMPLETED, savedJob.get().getStatus());
        assertEquals(originalKey, drawing.getPdfKey());
        assertEquals("tenants/org/uploaded/drawing/sample.webp", drawing.getThumbnailKey());
        verify(storagePort, never()).putObject(eq(originalKey), any(byte[].class), eq("application/pdf"));
        verify(organizationApi, times(1)).consumeStorageForCurrentTenant(anyLong());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).save(fileCaptor.capture());
        File thumbnailFile = fileCaptor.getValue();
        assertEquals("tenants/org/uploaded/drawing/sample.webp", thumbnailFile.getFileKey());
        assertEquals(drawingId, thumbnailFile.getOwnerId());
    }

    @Test
    void convertDrawing_이미지는_pdf와_썸네일을_생성한다() throws Exception {
        DrawingRepository drawingRepository = mock(DrawingRepository.class);
        DrawingProcessingJobRepository drawingProcessingJobRepository = mock(DrawingProcessingJobRepository.class);
        DrawingServingProjectionService drawingServingProjectionService = mock(DrawingServingProjectionService.class);
        FileRepository fileRepository = mock(FileRepository.class);
        StoragePort storagePort = mock(StoragePort.class);
        OrganizationApi organizationApi = mock(OrganizationApi.class);
        AtomicReference<DrawingProcessingJob> savedJob = new AtomicReference<>();

        String originalKey = "tenants/org/uploaded/drawing/sample.png";
        String pdfKey = "tenants/org/uploaded/drawing/sample.pdf";
        String thumbnailKey = "tenants/org/uploaded/drawing/sample.webp";

        Drawing drawing = Drawing.create(null, "sample.png");
        UUID drawingId = drawing.getId();
        drawing.changeOriginalFileKey(originalKey);
        drawing.markConversionPending();

        byte[] imageBytes = samplePng();
        File originalFile = File.create(UUID.randomUUID(), "sample.png", originalKey, "image/png", imageBytes.length);
        originalFile.markUploaded();
        originalFile.assignOwner("drawing", drawingId);

        when(drawingRepository.findById(drawingId)).thenReturn(Optional.of(drawing));
        when(fileRepository.findByFileKeyAndDeletedAtIsNull(originalKey)).thenReturn(Optional.of(originalFile));
        when(fileRepository.findByFileKeyAndDeletedAtIsNull(pdfKey)).thenReturn(Optional.empty());
        when(fileRepository.findByFileKeyAndDeletedAtIsNull(thumbnailKey)).thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingRepository.save(any(Drawing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingProcessingJobRepository.save(any(DrawingProcessingJob.class))).thenAnswer(invocation -> {
            DrawingProcessingJob job = invocation.getArgument(0);
            savedJob.set(job);
            return job;
        });
        when(drawingProcessingJobRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedJob.get()));
        when(drawingProcessingJobRepository.findByIdForUpdate(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedJob.get()));
        when(storagePort.getObject(originalKey)).thenReturn(imageBytes);
        doNothing().when(storagePort).putObject(eq(pdfKey), any(byte[].class), eq("application/pdf"));
        doNothing().when(storagePort).putObject(eq(thumbnailKey), any(byte[].class), eq("image/webp"));

        DrawingConversionService service = createService(
                drawingRepository,
                drawingProcessingJobRepository,
                drawingServingProjectionService,
                fileRepository,
                storagePort,
                organizationApi
        );

        service.requestAndConvertDrawing(drawingId);

        assertEquals(DrawingConversionStatus.COMPLETED, drawing.getConversionStatus());
        assertEquals(DrawingJobStatus.COMPLETED, savedJob.get().getStatus());
        assertEquals(pdfKey, drawing.getPdfKey());
        assertEquals(thumbnailKey, drawing.getThumbnailKey());
        verify(storagePort).putObject(eq(pdfKey), any(byte[].class), eq("application/pdf"));
        verify(storagePort).putObject(eq(thumbnailKey), any(byte[].class), eq("image/webp"));
        verify(organizationApi, times(2)).consumeStorageForCurrentTenant(anyLong());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository, times(2)).save(fileCaptor.capture());
        List<File> generatedFiles = fileCaptor.getAllValues();
        assertEquals(2, generatedFiles.size());
        assertEquals(pdfKey, generatedFiles.get(0).getFileKey());
        assertEquals(drawingId, generatedFiles.get(0).getOwnerId());
        assertEquals(thumbnailKey, generatedFiles.get(1).getFileKey());
        assertEquals(drawingId, generatedFiles.get(1).getOwnerId());
    }

    @Test
    void requestAndConvertDrawing_source_file이_없으면_failed로_마킹한다() {
        DrawingRepository drawingRepository = mock(DrawingRepository.class);
        DrawingProcessingJobRepository drawingProcessingJobRepository = mock(DrawingProcessingJobRepository.class);
        DrawingServingProjectionService drawingServingProjectionService = mock(DrawingServingProjectionService.class);
        FileRepository fileRepository = mock(FileRepository.class);
        StoragePort storagePort = mock(StoragePort.class);
        OrganizationApi organizationApi = mock(OrganizationApi.class);

        String originalKey = "tenants/org/uploaded/drawing/sample.pdf";

        Drawing drawing = Drawing.create(null, "sample.pdf");
        UUID drawingId = drawing.getId();
        drawing.changeOriginalFileKey(originalKey);
        drawing.markConversionPending();

        when(drawingRepository.findById(drawingId)).thenReturn(Optional.of(drawing));
        when(fileRepository.findByFileKeyAndDeletedAtIsNull(originalKey)).thenReturn(Optional.empty());
        when(drawingRepository.save(any(Drawing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DrawingConversionService service = createService(
                drawingRepository,
                drawingProcessingJobRepository,
                drawingServingProjectionService,
                fileRepository,
                storagePort,
                organizationApi
        );

        service.requestAndConvertDrawing(drawingId);

        assertEquals(DrawingConversionStatus.FAILED, drawing.getConversionStatus());
        verify(drawingProcessingJobRepository, never()).save(any(DrawingProcessingJob.class));
        verify(organizationApi, never()).consumeStorageForCurrentTenant(anyLong());
    }

    private DrawingConversionService createService(
            DrawingRepository drawingRepository,
            DrawingProcessingJobRepository drawingProcessingJobRepository,
            DrawingServingProjectionService drawingServingProjectionService,
            FileRepository fileRepository,
            StoragePort storagePort,
            OrganizationApi organizationApi
    ) {
        PdfBoxPdfPreviewRenderAdapter pdfPreviewRenderPort = new PdfBoxPdfPreviewRenderAdapter();
        return new DrawingConversionService(
                drawingRepository,
                fileRepository,
                storagePort,
                new DrawingConverterProperties("/opt/qcad", 1, tempDir.toString(), null, 300L, 420L, 24L),
                new DrawingArtifactService(fileRepository, storagePort, organizationApi),
                new DrawingProcessingJobService(drawingRepository, drawingProcessingJobRepository, drawingServingProjectionService),
                drawingServingProjectionService,
                new DrawingSourceClassifier(),
                new DrawingPipelineResolver(List.of(
                        new Pdf2dDrawingPipeline(pdfPreviewRenderPort),
                        new Raster2dDrawingPipeline(new PdfBoxRasterImageToPdfAdapter(), pdfPreviewRenderPort)
                ))
        );
    }

    private byte[] samplePdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] samplePng() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x336699);
        image.setRGB(7, 7, 0xFFFFFF);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
