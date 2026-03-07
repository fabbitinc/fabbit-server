package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartDefaultOwnerRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

  @Mock private PartRepository partRepository;
  @Mock private PartDefaultOwnerRepository partDefaultOwnerRepository;
  @Mock private FileRepository fileRepository;
  @Mock private OrganizationApi organizationApi;

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
            partRepository, partDefaultOwnerRepository, fileRepository, organizationApi);

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
            partRepository, partDefaultOwnerRepository, fileRepository, organizationApi);

    service.detachFile(part.getId(), file.getId());

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
}
