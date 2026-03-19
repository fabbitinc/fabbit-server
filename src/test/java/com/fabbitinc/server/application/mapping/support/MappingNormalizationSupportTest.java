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

class MappingNormalizationSupportTest {

    private final MappingNormalizationSupport support = new MappingNormalizationSupport();

    @Test
    void normalize_노드의_비표준속성을_확장속성으로_이동한다() {
        MappingResultDto normalized = support.normalize(new MappingResultDto(
                List.of(new NodeMappingDto(
                        "part_child",
                        "Part",
                        Map.of("part_number", "품번", "remark", "비고"),
                        List.of(new ExtendedPropertyMappingDto("탄소배출량", null, PropertyDataType.FLOAT)),
                        95,
                        "raw"
                )),
                List.of()
        ));

        NodeMappingDto node = normalized.nodes().get(0);
        assertEquals(Map.of("part_number", "품번"), node.propertyColumns());
        assertEquals(2, node.extendedProperties().size());
        assertEquals("_ext_remark", node.extendedProperties().get(0).generatedKey());
        assertEquals("_ext_탄소배출량", node.extendedProperties().get(1).generatedKey());
    }

    @Test
    void normalize_관계방향이_온톨로지와_다르면_제외한다() {
        MappingResultDto normalized = support.normalize(new MappingResultDto(
                List.of(
                        new NodeMappingDto("supplier_1", "Supplier", Map.of("company_name", "업체명"), List.of(), 90, "raw"),
                        new NodeMappingDto("part_1", "Part", Map.of("part_number", "품번"), List.of(), 90, "raw")
                ),
                List.of(
                        new RelationMappingDto(
                                "supplier_1",
                                RelationshipType.SUPPLIED_BY,
                                "part_1",
                                Map.of("remark", "비고"),
                                Map.of("remark", PropertyDataType.STRING),
                                List.of(),
                                80,
                                "raw"
                        )
                )
        ));

        assertEquals(0, normalized.relations().size());
    }
}
