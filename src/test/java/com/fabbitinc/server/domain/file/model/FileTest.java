package com.fabbitinc.server.domain.file.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileTest {

    @Test
    void file_문자열_필드는_trim_정규화한다() {
        File file = File.create(UUID.randomUUID(), "  a.txt  ", "  key/path  ", "  text/plain  ", 1L);

        assertEquals("a.txt", file.getOriginalName());
        assertEquals("key/path", file.getFileKey());
        assertEquals("text/plain", file.getContentType());
    }

    @Test
    void file_원본파일명이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                File.create(UUID.randomUUID(), " ", "key", "text/plain", 1L)
        );

        assertEquals(File.CODE_FILE_ORIGINAL_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void file_assignOwner_소유자ID가_null이면_예외를_던진다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);

        DomainException ex = assertThrows(DomainException.class, () -> file.assignOwner("issue", null));

        assertEquals(File.CODE_FILE_OWNER_ID_REQUIRED, ex.getDomainCode());
    }

    @Test
    void file_assignOwner_소유자타입은_trim_정규화한다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);
        UUID ownerId = UUID.randomUUID();

        file.markUploaded();

        file.assignOwner("  issue  ", ownerId);

        assertEquals("issue", file.getOwnerType());
        assertEquals(ownerId, file.getOwnerId());
    }

    @Test
    void file_업로드완료_후_소유자없으면_attachable이다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);

        file.markUploaded();

        assertTrue(file.isAttachable());
    }

    @Test
    void file_소유자가_있으면_attachable이_아니다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);
        file.markUploaded();
        file.assignOwner("issue", UUID.randomUUID());

        assertFalse(file.isAttachable());
    }

    @Test
    void file_assignOwner_업로드완료전이면_예외를_던진다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);

        DomainException ex = assertThrows(DomainException.class, () -> file.assignOwner("issue", UUID.randomUUID()));

        assertEquals(File.CODE_FILE_NOT_ATTACHABLE, ex.getDomainCode());
    }

    @Test
    void file_assignOwner_같은소유자로_재호출하면_noop이다() {
        File file = File.create("a.txt", "k", "text/plain", 1L);
        UUID ownerId = UUID.randomUUID();
        file.markUploaded();
        file.assignOwner("issue", ownerId);

        file.assignOwner("issue", ownerId);

        assertEquals("issue", file.getOwnerType());
        assertEquals(ownerId, file.getOwnerId());
    }
}
