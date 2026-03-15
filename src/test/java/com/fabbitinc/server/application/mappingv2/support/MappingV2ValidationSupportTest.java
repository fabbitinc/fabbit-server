package com.fabbitinc.server.application.mappingv2.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.application.mappingv2.model.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.model.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.model.RelationMappingV2Dto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingV2ValidationSupportTest {

    private final MappingV2ValidationSupport support = new MappingV2ValidationSupport();

    @Test
    void validate_없는컬럼과_숫자파싱경고를_반환한다() {
        MappingV2ResultDto mapping = new MappingV2ResultDto(
                List.of(
                        new NodeMappingV2Dto(
                                "part_child",
                                "Part",
                                Map.of("part_number", "품번"),
                                List.of(new ExtendedPropertyMappingV2Dto("탄소배출량", "_ext_carbon_emission", PropertyDataType.FLOAT)),
                                90,
                                "raw"
                        ),
                        new NodeMappingV2Dto(
                                "supplier_1",
                                "Supplier",
                                Map.of("company_name", "없는컬럼"),
                                List.of(),
                                90,
                                "raw"
                        )
                ),
                List.of(
                        new RelationMappingV2Dto(
                                "part_child",
                                RelationshipType.SUPPLIED_BY,
                                "supplier_1",
                                Map.of("unit_cost", "단가"),
                                Map.of("unit_cost", PropertyDataType.FLOAT),
                                List.of(),
                                80,
                                "raw"
                        )
                )
        );

        MappingV2ValidationSupport.ValidationResult result = support.validateAgainstRows(
                List.of("품번", "탄소배출량", "단가"),
                List.of(Map.of("품번", "P-001", "탄소배출량", "N/A", "단가", "10.5")),
                mapping
        );

        assertEquals(1, result.errors().size());
        assertEquals("MISSING_SOURCE_COLUMN", result.errors().get(0).code());
        assertEquals(1, result.warnings().size());
        assertEquals("NUMERIC_PARSE_WARNING", result.warnings().get(0).code());
    }
}
