package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartDefaultOwner;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

  @Mock private PartRepository partRepository;
  @Mock private PartDefaultOwnerRepository partDefaultOwnerRepository;
  @Mock private FileRepository fileRepository;
  @Mock private OrganizationApi organizationApi;
  @Mock private ObjectMapper objectMapper;
  @Mock private PartPreviewService partPreviewService;

  @Test
  void createPart_카테고리별_기본담당자를_적용한다() {
    UUID ownerId = UUID.randomUUID();
    UUID ownerTeamId = UUID.randomUUID();
    PartDefaultOwner defaultOwner = PartDefaultOwner.create("FASTENER", ownerId, ownerTeamId);
    when(partRepository.findByPartNumber("P-100")).thenReturn(Optional.empty());
    when(partDefaultOwnerRepository.findByCategory("FASTENER")).thenReturn(Optional.of(defaultOwner));
    when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PartService service =
        new PartService(
            partRepository,
            partDefaultOwnerRepository,
            fileRepository,
            organizationApi,
            objectMapper,
            partPreviewService);

    Part created =
        service.createPart(
            new CreatePartInput(
                "  P-100  ", "Bolt", null, null, null, "  FASTENER  ", null, null, null, null));

    assertEquals("P-100", created.getPartNumber());
    assertEquals("FASTENER", created.getCategory());
    assertEquals(ownerId, created.getOwnerId());
    assertEquals(ownerTeamId, created.getOwnerTeamId());
  }

  @Test
  void createPart_중복된_품번이면_conflict를_던진다() {
    when(partRepository.findByPartNumber("P-100")).thenReturn(Optional.of(Part.create("P-100", "Existing")));

    PartService service =
        new PartService(
            partRepository,
            partDefaultOwnerRepository,
            fileRepository,
            organizationApi,
            objectMapper,
            partPreviewService);

    AppException ex =
        assertThrows(
            AppException.class,
            () ->
                service.createPart(
                    new CreatePartInput(
                        "P-100", "Bolt", null, null, null, null, null, null, null, null)));

    assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
  }

  @Test
  void createPart_속성과_수명주기상태를_저장한다() throws Exception {
    Map<String, Object> extendedProperties = Map.of("weight", 1.2, "material_code", "AL6061");
    when(partRepository.findByPartNumber("P-200")).thenReturn(Optional.empty());
    when(partDefaultOwnerRepository.findByCategoryIsNull()).thenReturn(Optional.empty());
    when(objectMapper.writeValueAsString(extendedProperties))
        .thenReturn("{\"weight\":1.2,\"material_code\":\"AL6061\"}");
    when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PartService service =
        new PartService(
            partRepository,
            partDefaultOwnerRepository,
            fileRepository,
            organizationApi,
            objectMapper,
            partPreviewService);

    Part created =
        service.createPart(
            new CreatePartInput(
                "P-200",
                "Bracket",
                "AL6061",
                "EA",
                "sample",
                null,
                false,
                "design",
                7,
                extendedProperties));

    assertEquals("AL6061", created.getMaterial());
    assertEquals("EA", created.getUnit());
    assertEquals("sample", created.getDescription());
    assertEquals(Boolean.FALSE, created.getPhantom());
    assertEquals(PartLifecycleState.DESIGN, created.getLifecycleState());
    assertEquals(7, created.getLeadTimeDays());
    assertEquals("{\"weight\":1.2,\"material_code\":\"AL6061\"}", created.getExtendedProperties());
  }

  @Test
  void attachFiles_파일_총합만큼_스토리지를_소비한다() {
    Part part = Part.create("P-100", "Bolt");
    File first = createUploadedFile("first.pdf", 200L);
    File second = createUploadedFile("second.pdf", 300L);
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
    when(fileRepository.findByIdIn(List.of(first.getId(), second.getId())))
        .thenReturn(List.of(first, second));

    PartService service =
        new PartService(
            partRepository,
            partDefaultOwnerRepository,
            fileRepository,
            organizationApi,
            objectMapper,
            partPreviewService);

    List<File> attachedFiles =
        service.attachFiles(part.getId(), List.of(first.getId(), second.getId()));

    assertEquals(part.getId(), first.getOwnerId());
    assertEquals(part.getId(), second.getOwnerId());
    assertEquals(2, attachedFiles.size());
    verify(organizationApi).consumeStorageForCurrentTenant(500L);
  }

  @Test
  void detachFile_파일_크기만큼_스토리지를_반환한다() {
    Part part = Part.create("P-100", "Bolt");
    File file = createUploadedFile("first.pdf", 200L);
    file.assignOwner("part", part.getId());
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
    when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
            file.getId(), "part", part.getId()))
        .thenReturn(Optional.of(file));

    PartService service =
        new PartService(
            partRepository,
            partDefaultOwnerRepository,
            fileRepository,
            organizationApi,
            objectMapper,
            partPreviewService);

    service.detachFile(part.getId(), file.getId());

    assertNotNull(file.getDeletedAt());
    verify(partPreviewService).clearByFile(file.getId());
    verify(organizationApi).releaseStorageForCurrentTenant(200L);
  }

  private File createUploadedFile(String originalName, long fileSize) {
    File file =
        File.create(
            UUID.randomUUID(),
            originalName,
            "tenants/org/" + originalName,
            "application/pdf",
            fileSize);
    file.markUploaded();
    return file;
  }
}
