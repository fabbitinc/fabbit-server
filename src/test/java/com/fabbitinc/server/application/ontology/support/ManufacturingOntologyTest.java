package com.fabbitinc.server.application.ontology.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManufacturingOntologyTest {

    @Test
    void ontology가_의미메타데이터를_핵심정의로_보유한다() {
        ManufacturingOntology.NodeLabelDef part = ManufacturingOntology.ONTOLOGY.getNodeLabel("Part");
        ManufacturingOntology.PropertyDef partNumber = part.properties().stream()
                .filter(property -> property.name().equals("part_number"))
                .findFirst()
                .orElseThrow();

        assertEquals("제조 공정에서 관리되는 개별 부품 또는 조립품. 완제품, 반제품, 원자재, 구매품 모두 포함. BOM(Bill of Materials) 구조에서 상위/하위 관계의 기본 단위.", part.semanticDescription());
        assertEquals("부품의 고유 식별자로 조직 내에서 유일해야 함", partNumber.semanticDescription());
        assertTrue(partNumber.aliases().contains("'Part No.'"));
        assertTrue(partNumber.examples().contains("'ASM-001'"));
        assertEquals(List.of("part_number"), part.mergeKeys());
    }
}
