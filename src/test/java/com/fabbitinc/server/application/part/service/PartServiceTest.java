package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.input.CreatePartInput;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

  @Mock private PartRepository partRepository;
  @Mock private PartRevisionRepository partRevisionRepository;
  @Mock private FileRepository fileRepository;
  @Mock private OrganizationApi organizationApi;
  @Mock private PropertyApi propertyApi;
  @Mock private ObjectMapper objectMapper;

  @Test
  void createPart_초기_초안을_생성한다() {
    UUID actorId = UUID.randomUUID();
    when(partRepository.findByPartNumber("P-100")).thenReturn(Optional.empty());
    when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(partRevisionRepository.save(any(PartRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PartService service = createService();

    PartRevision createdDraft =
        service.createPart(
            new CreatePartInput(
                "  P-100  ", "Bolt", null, null, null, "  FASTENER  ", null, null, null, null, null),
            actorId);

    ArgumentCaptor<PartRevision> revisionCaptor = ArgumentCaptor.forClass(PartRevision.class);
    verify(partRevisionRepository).save(revisionCaptor.capture());
    PartRevision savedRevision = revisionCaptor.getValue();

    assertEquals("P-100", createdDraft.getPartNumber());
    assertEquals(null, createdDraft.getRevisionCode());
    assertEquals("Bolt", savedRevision.getName());
    assertEquals("FASTENER", savedRevision.getCategory());
    ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
    verify(partRepository).save(partCaptor.capture());
    Part savedPart = partCaptor.getValue();
    assertEquals(PartLifecycleState.ACTIVE, savedPart.getLifecycleState());
  }

  @Test
  void createPart_중복된_품번이면_conflict를_던진다() {
    when(partRepository.findByPartNumber("P-100")).thenReturn(Optional.of(Part.create("P-100")));

    PartService service = createService();

    AppException ex =
        assertThrows(
            AppException.class,
            () ->
                service.createPart(
                    new CreatePartInput(
                        "P-100", "Bolt", null, null, null, null, null, null, null, null, null),
                    UUID.randomUUID()));

    assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
  }

  @Test
  void createPart_속성과_수명주기상태를_저장한다() throws Exception {
    UUID actorId = UUID.randomUUID();
    Map<String, Object> extendedProperties = Map.of("weight", 1.2, "material_code", "AL6061");
    when(partRepository.findByPartNumber("P-200")).thenReturn(Optional.empty());
    when(propertyApi.validateExtendedProperties(com.fabbitinc.server.domain.property.model.PropertyOwnerType.PART, extendedProperties))
        .thenReturn(extendedProperties);
    when(objectMapper.writeValueAsString(extendedProperties))
        .thenReturn("{\"weight\":1.2,\"material_code\":\"AL6061\"}");
    when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(partRevisionRepository.save(any(PartRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PartService service = createService();

    PartRevision createdDraft =
        service.createPart(
            new CreatePartInput(
                "P-200",
                "Bracket",
                "AL6061",
                "EA",
                "sample",
                null,
                false,
                PartLifecycleState.ACTIVE,
                7,
                extendedProperties,
                null),
            actorId);

    ArgumentCaptor<PartRevision> revisionCaptor = ArgumentCaptor.forClass(PartRevision.class);
    verify(partRevisionRepository).save(revisionCaptor.capture());
    PartRevision savedRevision = revisionCaptor.getValue();

    assertEquals(null, createdDraft.getRevisionCode());
    ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
    verify(partRepository).save(partCaptor.capture());
    Part savedPart = partCaptor.getValue();
    assertEquals(PartLifecycleState.ACTIVE, savedPart.getLifecycleState());
    assertEquals("Bracket", savedRevision.getName());
    assertEquals("AL6061", savedRevision.getMaterial());
    assertEquals("EA", savedRevision.getUnit());
    assertEquals("sample", savedRevision.getDescription());
    assertEquals(Boolean.FALSE, savedRevision.getPhantom());
    assertEquals(7, savedRevision.getLeadTimeDays());
    assertEquals("{\"weight\":1.2,\"material_code\":\"AL6061\"}", savedRevision.getExtendedProperties());
  }

  @Test
  void attachFiles_파일_총합만큼_스토리지를_소비한다() {
    Part part = Part.create("P-100");
    PartRevision revision = PartRevision.createInitialDraft(part, "Part", null);
    File first = createUploadedFile("first.pdf", 200L);
    File second = createUploadedFile("second.pdf", 300L);
    when(partRevisionRepository.findByIdAndPartId(revision.getId(), part.getId())).thenReturn(Optional.of(revision));
    when(fileRepository.findByIdIn(List.of(first.getId(), second.getId())))
        .thenReturn(List.of(first, second));

    PartService service = createService();

    List<File> attachedFiles =
        service.attachFiles(part.getId(), revision.getId(), List.of(first.getId(), second.getId()));

    assertEquals(revision.getId(), first.getOwnerId());
    assertEquals(revision.getId(), second.getOwnerId());
    assertEquals(2, attachedFiles.size());
    verify(organizationApi).consumeStorageForCurrentTenant(500L);
  }

  @Test
  void detachFile_파일_크기만큼_스토리지를_반환한다() {
    Part part = Part.create("P-100");
    PartRevision revision = PartRevision.createInitialDraft(part, "Part", null);
    File file = createUploadedFile("first.pdf", 200L);
    file.assignOwner("part_revision", revision.getId());
    when(partRevisionRepository.findByIdAndPartId(revision.getId(), part.getId())).thenReturn(Optional.of(revision));
    when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
            file.getId(), "part_revision", revision.getId()))
        .thenReturn(Optional.of(file));

    PartService service = createService();

    service.detachFile(part.getId(), revision.getId(), file.getId(), UUID.randomUUID());

    assertNotNull(file.getDeletedAt());
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

  private PartService createService() {
    return new PartService(
        partRepository,
        partRevisionRepository,
        fileRepository,
        organizationApi,
        propertyApi,
        objectMapper);
  }
}
