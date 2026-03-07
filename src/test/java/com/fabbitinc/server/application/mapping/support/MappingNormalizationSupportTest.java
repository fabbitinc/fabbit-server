package com.fabbitinc.server.application.mapping.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.common.PropertyMappingDto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import java.util.List;
import org.junit.jupiter.api.Test;

class MappingNormalizationSupportTest {

    private final MappingNormalizationSupport support = new MappingNormalizationSupport();

    @Test
    void normalize_확장속성명을_python원본처럼_보정한다() {
        MappingResultDto normalized = support.normalize(new MappingResultDto(
                List.of(
                        new PropertyMappingDto("탄소배출량", "탄소배출량", "_ext_carbon_emission", PropertyDataType.STRING, 90, "raw", false),
                        new PropertyMappingDto("비고", "_ext__ext_custom_value__", null, PropertyDataType.STRING, 80, "raw", false)
                ),
                List.of()
        ));

        assertEquals("_ext_탄소배출량", normalized.propertyMappings().get(0).targetProperty());
        assertEquals("_ext_carbon_emission", normalized.propertyMappings().get(0).suggestedExtendedProperty());
        assertEquals("_ext_custom_value", normalized.propertyMappings().get(1).targetProperty());
        assertEquals("_ext_custom_value", normalized.propertyMappings().get(1).suggestedExtendedProperty());
    }

    @Test
    void normalize_빈확장속성은_unknown으로_보정한다() {
        MappingResultDto normalized = support.normalize(new MappingResultDto(
                List.of(new PropertyMappingDto("임의컬럼", "", null, PropertyDataType.STRING, 50, "raw", false)),
                List.of()
        ));

        assertEquals("_ext_unknown", normalized.propertyMappings().get(0).targetProperty());
        assertEquals("_ext_unknown", normalized.propertyMappings().get(0).suggestedExtendedProperty());
    }

    @Test
    void normalize_표준속성에도_확장속성_대안을_채운다() {
        MappingResultDto normalized = support.normalize(new MappingResultDto(
                List.of(new PropertyMappingDto("품번", "part_number", null, PropertyDataType.STRING, 95, "raw", false)),
                List.of()
        ));

        assertEquals("part_number", normalized.propertyMappings().get(0).targetProperty());
        assertEquals("_ext_part_number", normalized.propertyMappings().get(0).suggestedExtendedProperty());
    }
}
