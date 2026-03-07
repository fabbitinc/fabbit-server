package com.fabbitinc.server.application.file.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @Mock private FileRepository fileRepository;
  @Mock private StoragePort storagePort;
  @Mock private OrganizationApi organizationApi;

  @Test
  void softDelete_연결된_업로드완료_파일은_스토리지_사용량을_반환한다() {
    File file =
        File.create(
            UUID.randomUUID(), "sample.pdf", "tenants/org/sample.pdf", "application/pdf", 512L);
    file.markUploaded();
    file.assignOwner("part", UUID.randomUUID());
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

    FileService service = new FileService(fileRepository, storagePort, organizationApi);

    service.softDelete(file.getId());

    assertNotNull(file.getDeletedAt());
    verify(organizationApi).releaseStorageForCurrentTenant(512L);
  }

  @Test
  void softDelete_미연결_파일은_스토리지_사용량을_반환하지_않는다() {
    File file =
        File.create(
            UUID.randomUUID(), "sample.pdf", "tenants/org/sample.pdf", "application/pdf", 512L);
    file.markUploaded();
    when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

    FileService service = new FileService(fileRepository, storagePort, organizationApi);

    service.softDelete(file.getId());

    assertNotNull(file.getDeletedAt());
    verify(organizationApi, never()).releaseStorageForCurrentTenant(512L);
  }
}
