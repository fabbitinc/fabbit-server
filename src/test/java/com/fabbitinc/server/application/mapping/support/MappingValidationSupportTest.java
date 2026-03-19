package com.fabbitinc.server.application.mapping.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.application.mapping.model.ExtendedPropertyMappingDto;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.NodeMappingDto;
import com.fabbitinc.server.application.mapping.model.RelationMappingDto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingValidationSupportTest {

    private final MappingValidationSupport support = new MappingValidationSupport();

    @Test
    void validate_없는컬럼과_숫자파싱경고를_반환한다() {
        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new NodeMappingDto(
                                "part_child",
                                "Part",
                                Map.of("part_number", "품번"),
                                List.of(new ExtendedPropertyMappingDto("탄소배출량", "_ext_carbon_emission", PropertyDataType.FLOAT)),
                                90,
                                "raw"
                        ),
                        new NodeMappingDto(
                                "supplier_1",
                                "Supplier",
                                Map.of("company_name", "없는컬럼"),
                                List.of(),
                                90,
                                "raw"
                        )
                ),
                List.of(
                        new RelationMappingDto(
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

        MappingValidationSupport.ValidationResult result = support.validateAgainstRows(
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
