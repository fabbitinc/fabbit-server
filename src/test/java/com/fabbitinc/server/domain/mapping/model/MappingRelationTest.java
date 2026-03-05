package com.fabbitinc.server.domain.mapping.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingRelationTest {

    @Test
    void mappingRevision_엔티티_입력시_FK와_연관을_동기화한다() {
        MappingRecord record = new MappingRecord("기본 매핑", MappingScope.PART_LIST);
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 256L);

        MappingRevision revision = MappingRevision.create(
                record,
                file,
                1,
                "Sheet1",
                "[\"A\",\"B\"]",
                "{\"partNumber\":\"A\"}"
        );

        assertEquals(record, revision.getRecord());
        assertEquals(file, revision.getFile());
        assertEquals(record.getId(), revision.getRecordId());
        assertEquals(file.getId(), revision.getFileId());
        assertEquals(1, record.getRevisions().size());
        assertTrue(record.getRevisions().contains(revision));
    }

    @Test
    void mappingRevision_레코드가_null이면_예외를_던진다() {
        File file = new File("sample.csv", "files/sample.csv", "text/csv", 256L);

        DomainException ex = assertThrows(DomainException.class, () -> MappingRevision.create(
                null,
                file,
                1,
                "Sheet1",
                "[]",
                "{}"
        ));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_RECORD_REQUIRED, ex.getDomainCode());
    }

    @Test
    void mappingRevision_파일이_null이면_예외를_던진다() {
        MappingRecord record = new MappingRecord("기본 매핑", MappingScope.PART_LIST);

        DomainException ex = assertThrows(DomainException.class, () -> MappingRevision.create(
                record,
                null,
                1,
                "Sheet1",
                "[]",
                "{}"
        ));

        assertEquals(MappingRevision.CODE_MAPPING_REVISION_FILE_REQUIRED, ex.getDomainCode());
    }
}
