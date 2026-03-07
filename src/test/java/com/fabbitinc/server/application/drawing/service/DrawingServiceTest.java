package com.fabbitinc.server.application.drawing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrawingServiceTest {

  @Mock private DrawingRepository drawingRepository;
  @Mock private FileRepository fileRepository;
  @Mock private DrawingAsyncConversionService drawingAsyncConversionService;
  @Mock private OrganizationApi organizationApi;

  @AfterEach
  void clearTenantContext() {
    com.fabbitinc.server.application.tenant.support.TenantContextHolder.clear();
  }

  @Test
  void createDrawing_원본_파일_크기만큼_스토리지를_소비한다() {
    File file =
        File.create(
            UUID.randomUUID(),
            "sample.pdf",
            "tenants/org/uploaded/sample.pdf",
            "application/pdf",
            512L);
    file.markUploaded();
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));
    when(drawingRepository.save(any(Drawing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DrawingService service =
        new DrawingService(
            drawingRepository, fileRepository, drawingAsyncConversionService, organizationApi);

    Drawing drawing = service.createDrawing(file.getId());

    assertEquals(drawing.getId(), file.getOwnerId());
    verify(organizationApi).consumeStorageForCurrentTenant(512L);
    verify(drawingAsyncConversionService).convertDrawingAsync(drawing.getId(), "public");
  }

  @Test
  void deleteDrawing_원본과_생성파일_모두_스토리지를_반환한다() {
    Drawing drawing = Drawing.create("D-100", "sample.pdf");
    drawing.changeOriginalFileKey("tenants/org/uploaded/sample.pdf");
    drawing.markConversionCompleted(
        "tenants/org/uploaded/sample.pdf", "tenants/org/uploaded/sample_thumbnail.png");
    drawing.changePdfKey("tenants/org/uploaded/sample_generated.pdf");

    File original =
        createOwnedFile("sample.pdf", drawing.getId(), "tenants/org/uploaded/sample.pdf", 100L);
    File pdf =
        createOwnedFile(
            "sample_generated.pdf",
            drawing.getId(),
            "tenants/org/uploaded/sample_generated.pdf",
            200L);
    File thumbnail =
        createOwnedFile(
            "sample_thumbnail.png",
            drawing.getId(),
            "tenants/org/uploaded/sample_thumbnail.png",
            50L);

    when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));
    when(fileRepository.findByFileKeyAndDeletedAtIsNull(original.getFileKey()))
        .thenReturn(Optional.of(original));
    when(fileRepository.findByFileKeyAndDeletedAtIsNull(pdf.getFileKey()))
        .thenReturn(Optional.of(pdf));
    when(fileRepository.findByFileKeyAndDeletedAtIsNull(thumbnail.getFileKey()))
        .thenReturn(Optional.of(thumbnail));

    DrawingService service =
        new DrawingService(
            drawingRepository, fileRepository, drawingAsyncConversionService, organizationApi);

    service.deleteDrawing(drawing.getId());

    assertNotNull(original.getDeletedAt());
    assertNotNull(pdf.getDeletedAt());
    assertNotNull(thumbnail.getDeletedAt());
    verify(organizationApi).releaseStorageForCurrentTenant(100L);
    verify(organizationApi).releaseStorageForCurrentTenant(200L);
    verify(organizationApi).releaseStorageForCurrentTenant(50L);
  }

  private File createOwnedFile(String originalName, UUID drawingId, String fileKey, long fileSize) {
    File file =
        File.create(UUID.randomUUID(), originalName, fileKey, "application/octet-stream", fileSize);
    file.markUploaded();
    file.assignOwner("drawing", drawingId);
    return file;
  }
}
