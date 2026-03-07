package com.fabbitinc.server.application.mappingv2.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.application.mappingv2.dto.common.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.RelationMappingV2Dto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingV2NormalizationSupportTest {

    private final MappingV2NormalizationSupport support = new MappingV2NormalizationSupport();

    @Test
    void normalize_노드의_비표준속성을_확장속성으로_이동한다() {
        MappingV2ResultDto normalized = support.normalize(new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto(
                        "part_child",
                        "Part",
                        Map.of("part_number", "품번", "remark", "비고"),
                        List.of(new ExtendedPropertyMappingV2Dto("탄소배출량", null, PropertyDataType.FLOAT)),
                        95,
                        "raw"
                )),
                List.of()
        ));

        NodeMappingV2Dto node = normalized.nodes().get(0);
        assertEquals(Map.of("part_number", "품번"), node.propertyColumns());
        assertEquals(2, node.extendedProperties().size());
        assertEquals("_ext_remark", node.extendedProperties().get(0).generatedKey());
        assertEquals("_ext_탄소배출량", node.extendedProperties().get(1).generatedKey());
    }

    @Test
    void normalize_관계방향이_온톨로지와_다르면_제외한다() {
        MappingV2ResultDto normalized = support.normalize(new MappingV2ResultDto(
                List.of(
                        new NodeMappingV2Dto("supplier_1", "Supplier", Map.of("company_name", "업체명"), List.of(), 90, "raw"),
                        new NodeMappingV2Dto("part_1", "Part", Map.of("part_number", "품번"), List.of(), 90, "raw")
                ),
                List.of(
                        new RelationMappingV2Dto(
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
