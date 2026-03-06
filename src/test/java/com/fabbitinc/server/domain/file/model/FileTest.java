package com.fabbitinc.server.domain.file.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTest {

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
}
