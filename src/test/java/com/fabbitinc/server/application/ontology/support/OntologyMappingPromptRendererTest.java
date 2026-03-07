package com.fabbitinc.server.application.ontology.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologyMappingPromptRendererTest {

    @Test
    void renderer가_기존과_같은_매핑가이드텍스트를_생성한다() {
        String promptText = OntologyMappingPromptRenderer.render(ManufacturingOntology.ONTOLOGY);

        assertTrue(promptText.contains("## Part (제조 공정에서 관리되는 개별 부품 또는 조립품."));
        assertTrue(promptText.contains("Excel에서 '품번', '부품번호', 'Part No.', 'P/N' 등으로 표기될 수 있음"));
        assertTrue(promptText.contains("예: 'ASM-001', 'PRT-1234', 'M-BOLT-10'"));
        assertTrue(promptText.contains("### Part -[CONSISTS_OF]-> Part"));
        assertTrue(promptText.contains("관계 속성 (rel_columns 매핑 대상):"));
    }
}
