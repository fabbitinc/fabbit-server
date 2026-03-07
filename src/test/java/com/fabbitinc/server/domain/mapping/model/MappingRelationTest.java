package com.fabbitinc.server.domain.mapping.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import org.junit.jupiter.api.Test;

class MappingRelationTest {

    @Test
    void mappingRecord_createRevision이_리비전을_생성하고_컬렉션에_추가한다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);

        MappingRevision revision = record.createRevision(
                file.getId(),
                "Sheet1",
                "[\"A\",\"B\"]",
                "{\"partNumber\":\"A\"}"
        );

        assertEquals(record, revision.getRecord());
        assertEquals(record.getId(), revision.getRecordId());
        assertEquals(file.getId(), revision.getFileId());
        assertEquals(1, revision.getVersion());
        assertEquals(1, record.getRevisions().size());
        assertTrue(record.getRevisions().contains(revision));
    }

    @Test
    void mappingRecord_createRevision_파일이_null이면_예외를_던진다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);

        DomainException ex = assertThrows(DomainException.class, () -> record.createRevision(
                null,
                "Sheet1",
                "[]",
                "{}"
        ));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_FILE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void mappingRevision_버전이_0이면_예외를_던진다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);

        DomainException ex = assertThrows(DomainException.class, () -> MappingRevision.create(
                record,
                file.getId(),
                0,
                "Sheet1",
                "[]",
                "{}"
        ));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_VERSION_INVALID, ex.getDomainCode());
    }

    @Test
    void mappingRevision_sheetName은_trim_정규화한다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);

        MappingRevision revision = record.createRevision(
                file.getId(),
                "  Sheet A  ",
                "[]",
                "{}"
        );

        assertEquals("Sheet A", revision.getSheetName());
    }

    @Test
    void mappingRevision_sheetName이_blank면_null로_정규화한다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);

        MappingRevision revision = record.createRevision(
                file.getId(),
                "   ",
                "[]",
                "{}"
        );

        assertNull(revision.getSheetName());
    }

    @Test
    void mappingRevision_sheetName이_너무_길면_예외를_던진다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);

        DomainException ex = assertThrows(DomainException.class, () -> record.createRevision(
                file.getId(),
                "a".repeat(201),
                "[]",
                "{}"
        ));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_SHEET_NAME_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void mappingRevision_incrementUsage_0이하면_예외를_던진다() {
        MappingRecord record = MappingRecord.create("기본 매핑", MappingScope.PART_LIST);
        File file = File.create("sample.csv", "files/sample.csv", "text/csv", 256L);
        MappingRevision revision = record.createRevision(file.getId(), null, "[]", "{}");

        DomainException ex = assertThrows(DomainException.class, () -> revision.incrementUsage(0));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_USAGE_INCREMENT_INVALID, ex.getDomainCode());
    }
}
