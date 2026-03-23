package com.fabbitinc.server.application.drawing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
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
  @Mock private StoragePort storagePort;
  @Mock private OrganizationApi organizationApi;
  @Mock private PartApi partApi;

  @AfterEach
  void clearTenantContext() {
    com.fabbitinc.server.application.tenant.support.TenantContextHolder.clear();
  }

  @Test
  void createDrawing_원본_파일_크기만큼_스토리지를_소비한다() {
    UUID partRevisionId = UUID.randomUUID();
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
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    Drawing drawing = service.createDrawing(partRevisionId, file.getId());

    assertEquals(drawing.getId(), file.getOwnerId());
    assertEquals(partRevisionId, drawing.getPartRevisionId());
    verify(organizationApi).consumeStorageForCurrentTenant(512L);
  }

  @Test
  void createDrawing_DWG도면은_등록할_수_있다() {
    UUID partRevisionId = UUID.randomUUID();
    File file =
        File.create(
            UUID.randomUUID(),
            "sample.dwg",
            "tenants/org/uploaded/sample.dwg",
            "application/acad",
            256L);
    file.markUploaded();
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));
    when(drawingRepository.save(any(Drawing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DrawingService service =
        new DrawingService(
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    Drawing drawing = service.createDrawing(partRevisionId, file.getId());

    assertEquals(DrawingSourceType.CAD_2D, drawing.getSourceType());
    assertEquals(DrawingDimension.TWO_D, drawing.getDimension());
    assertEquals("tenants/org/uploaded/sample.dwg", drawing.getOriginalFileKey());
  }

  @Test
  void createDrawing_업로드미완료파일도_스토리지에_있으면_완료처리후_등록한다() {
    UUID partRevisionId = UUID.randomUUID();
    File file =
        File.create(
            UUID.randomUUID(),
            "sample.step",
            "tenants/org/uploaded/sample.step",
            "model/step",
            256L);
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));
    when(storagePort.headObject(file.getFileKey())).thenReturn(org.mockito.Mockito.mock(com.fabbitinc.server.application.file.port.StorageObjectMeta.class));
    when(drawingRepository.save(any(Drawing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DrawingService service =
        new DrawingService(
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    Drawing drawing = service.createDrawing(partRevisionId, file.getId());

    assertEquals(drawing.getId(), file.getOwnerId());
    assertEquals(com.fabbitinc.server.domain.file.model.FileStatus.UPLOADED, file.getStatus());
  }

  @Test
  void createDrawing_이미_다른_리소스에_연결된_파일이면_거부된다() {
    File file =
        File.create(
            UUID.randomUUID(),
            "sample.pdf",
            "tenants/org/uploaded/sample.pdf",
            "application/pdf",
            100L);
    file.markUploaded();
    file.assignOwner("part_revision", UUID.randomUUID());
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

    DrawingService service =
        new DrawingService(
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    AppException ex = assertThrows(AppException.class, () -> service.createDrawing(UUID.randomUUID(), file.getId()));

    assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
  }

  @Test
  void deleteDrawing_원본_파일_스토리지를_반환한다() {
    Drawing drawing = Drawing.create("D-100", "sample.pdf");
    drawing.assignSourceFile(UUID.randomUUID(), DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
    drawing.changeOriginalFileKey("tenants/org/uploaded/sample.pdf");

    File original =
        createOwnedFile("sample.pdf", drawing.getId(), "tenants/org/uploaded/sample.pdf", 100L);

    when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));
    when(fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId())).thenReturn(Optional.of(original));
    when(fileRepository.findByFileKeyAndDeletedAtIsNull(original.getFileKey()))
        .thenReturn(Optional.of(original));

    DrawingService service =
        new DrawingService(
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    service.deleteDrawing(drawing.getId(), UUID.randomUUID());

    assertNotNull(original.getDeletedAt());
    verify(partApi).clearPreviewByDrawing(drawing.getId());
    verify(organizationApi).releaseStorageForCurrentTenant(100L);
  }

  @Test
  void deleteDrawing_source파일행이_없어도_originalFileKey로_정리한다() {
    Drawing drawing = Drawing.create("D-101", "sample.pdf");
    drawing.assignSourceFile(UUID.randomUUID(), DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
    drawing.changeOriginalFileKey("tenants/org/uploaded/sample.pdf");

    File original =
        createOwnedFile("sample.pdf", drawing.getId(), "tenants/org/uploaded/sample.pdf", 100L);

    when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));
    when(fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId())).thenReturn(Optional.empty());
    when(fileRepository.findByFileKeyAndDeletedAtIsNull(original.getFileKey()))
        .thenReturn(Optional.of(original));

    DrawingService service =
        new DrawingService(
            drawingRepository,
            fileRepository,
            storagePort,
            organizationApi,
            partApi);

    service.deleteDrawing(drawing.getId(), UUID.randomUUID());

    assertNotNull(original.getDeletedAt());
    verify(organizationApi).releaseStorageForCurrentTenant(100L);
  }

  private File createOwnedFile(String originalName, UUID drawingId, String fileKey, long fileSize) {
    File file =
        File.create(UUID.randomUUID(), originalName, fileKey, "application/octet-stream", fileSize);
    file.markUploaded();
    file.assignOwner("drawing", drawingId);
    return file;
  }
}
