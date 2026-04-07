package com.fabbitinc.server.application.file.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.file.service.input.CreateFileInput;
import com.fabbitinc.server.application.file.service.output.BatchCreateFilesOutput;
import com.fabbitinc.server.application.file.service.output.CreateFileOutput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private StoragePort storagePort;
    @Mock
    private OrganizationApi organizationApi;

    @Test
    void batchCreateFiles_일반배치업로드는_uploaded_경로를_사용한다() {
        when(storagePort.generateUploadPresignedUrl(any(), any())).thenReturn("https://upload.example");

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        BatchCreateFilesOutput output = service.batchCreateFiles(
                auth(),
                java.util.List.of(new CreateFileInput("sample.csv", "text/csv", 100L, null))
        );

        CreateFileOutput item = output.items().getFirst();
        assertTrue(item.fileKey().contains("/uploaded/"));
        verify(storagePort).generateUploadPresignedUrl(eq(item.fileKey()), eq("text/csv"));
    }

    @Test
    void batchCreateRawFiles_마이그레이션원본업로드는_raw_data_경로를_사용한다() {
        when(storagePort.generateUploadPresignedUrl(any(), any())).thenReturn("https://upload.example");

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        BatchCreateFilesOutput output = service.batchCreateRawFiles(
                auth(),
                java.util.List.of(new CreateFileInput("source.ipt", "application/octet-stream", 100L, null))
        );

        CreateFileOutput item = output.items().getFirst();
        assertTrue(item.fileKey().contains("/raw_data/"));
        verify(storagePort).generateUploadPresignedUrl(eq(item.fileKey()), eq("application/octet-stream"));
    }

    @Test
    void convertToThumbnail_png를_webp로_실제변환하고_원본을_삭제한다() throws Exception {
        File file = File.create(
                UUID.randomUUID(),
                "profile.png",
                "tenants/org/uploaded/profile.png",
                "image/png",
                512L
        );
        byte[] originalBytes = createPngImage(400, 200);
        when(storagePort.getObject(file.getFileKey())).thenReturn(originalBytes);

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        service.convertToThumbnail(file);

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storagePort).putObject(eq("tenants/org/uploaded/profile.webp"), contentCaptor.capture(), eq("image/webp"));
        verify(storagePort).deleteObject("tenants/org/uploaded/profile.png");
        assertEquals("tenants/org/uploaded/profile.webp", file.getFileKey());
        assertEquals("image/webp", file.getContentType());
        assertEquals(contentCaptor.getValue().length, file.getFileSize());
        assertWebpThumbnail(contentCaptor.getValue(), 256, 256);
    }

    @Test
    void convertToThumbnail_기존키가_webp면_같은키로_덮어쓰고_삭제하지_않는다() {
        File file = File.create(
                UUID.randomUUID(),
                "profile.webp",
                "tenants/org/uploaded/profile.webp",
                "image/webp",
                512L
        );
        when(storagePort.getObject(file.getFileKey())).thenReturn(createPngImage(128, 256));

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        service.convertToThumbnail(file);

        verify(storagePort).putObject(eq("tenants/org/uploaded/profile.webp"), any(byte[].class), eq("image/webp"));
        verify(storagePort, never()).deleteObject(any());
        assertEquals("tenants/org/uploaded/profile.webp", file.getFileKey());
        assertEquals("image/webp", file.getContentType());
    }

    @Test
    void convertToThumbnail_지원하지않는_이미지는_예외를_던진다() {
        File file = File.create(
                UUID.randomUUID(),
                "profile.bin",
                "tenants/org/uploaded/profile.bin",
                "application/octet-stream",
                16L
        );
        when(storagePort.getObject(file.getFileKey())).thenReturn(new byte[] {1, 2, 3, 4});

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        AppException exception = assertThrows(AppException.class, () -> service.convertToThumbnail(file));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(storagePort, never()).putObject(any(), any(byte[].class), any());
        verify(storagePort, never()).deleteObject(any());
    }

    @Test
    void softDelete_연결된_업로드완료_파일은_스토리지_사용량을_반환한다() {
        File file = File.create(
                UUID.randomUUID(),
                "sample.pdf",
                "tenants/org/sample.pdf",
                "application/pdf",
                512L
        );
        file.markUploaded();
        file.assignOwner("part", UUID.randomUUID());
        when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        service.softDelete(file.getId(), UUID.randomUUID());

        assertNotNull(file.getDeletedAt());
        verify(organizationApi).releaseStorageForCurrentTenant(512L);
    }

    @Test
    void softDelete_미연결_파일은_스토리지_사용량을_반환하지_않는다() {
        File file = File.create(
                UUID.randomUUID(),
                "sample.pdf",
                "tenants/org/sample.pdf",
                "application/pdf",
                512L
        );
        file.markUploaded();
        when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

        FileService service = new FileService(fileRepository, storagePort, organizationApi);

        service.softDelete(file.getId(), UUID.randomUUID());

        assertNotNull(file.getDeletedAt());
        verify(organizationApi, never()).releaseStorageForCurrentTenant(512L);
    }

    private byte[] createPngImage(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, x < width / 2 ? Color.BLUE.getRGB() : Color.RED.getRGB());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void assertWebpThumbnail(byte[] imageBytes, int width, int height) throws Exception {
        assertArrayEquals(new byte[] {'R', 'I', 'F', 'F'}, new byte[] {
                imageBytes[0],
                imageBytes[1],
                imageBytes[2],
                imageBytes[3]
        });

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image);
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
    }

    private AuthContext auth() {
        return new AuthContext(UUID.randomUUID(), "user@fabbit.io", UUID.randomUUID(), MembershipRole.OWNER);
    }
}
