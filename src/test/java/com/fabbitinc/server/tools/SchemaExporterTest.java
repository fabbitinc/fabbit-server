package com.fabbitinc.server.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SchemaExporterTest {

    @Test
    void schemaExportTenant_public_참조가_있어도_성공하고_public_테이블은_제외한다() throws Exception {
        String ddl = SchemaExporter.exportSql("tenant");

        assertTrue(ddl.contains("create table activities"));
        assertTrue(ddl.contains("references public.users"));
        assertFalse(ddl.contains("create table public.users"));
    }

    @Test
    void schemaExportPublic_public_테이블만_출력한다() throws Exception {
        String ddl = SchemaExporter.exportSql("public");

        assertTrue(ddl.contains("create table public.users"));
        assertFalse(ddl.contains("create table activities"));
    }
}
