package com.fabbitinc.server.application.ontology.support;

import java.util.List;

public final class ManufacturingOntology {

    public static final OntologyDef ONTOLOGY = new OntologyDef(
            "Fabbit 제조업 온톨로지",
            "부품(Part), 도면(Drawing), 공급사(Supplier), 프로젝트(Project)와 이들 간의 BOM·도면·공급·소속 관계를 표현하는 제조 도메인 지식 그래프 스키마입니다.",
            List.of(
                    new NodeLabelDef(
                            "Part",
                            "제조 공정에서 관리되는 개별 부품 또는 조립품",
                            List.of(
                                    new PropertyDef("part_number", "품번", "string", true, true, true),
                                    new PropertyDef("name", "부품명", "string", true, false, false),
                                    new PropertyDef("revision", "리비전", "string", false, false, false),
                                    new PropertyDef("material", "재질", "string", false, false, false),
                                    new PropertyDef("unit", "단위", "string", false, false, false),
                                    new PropertyDef("description", "설명", "string", false, false, false),
                                    new PropertyDef("category", "분류", "string", false, false, false),
                                    new PropertyDef("is_phantom", "팬텀 조립품 여부", "boolean", false, false, false),
                                    new PropertyDef("lifecycle_state", "수명주기 상태", "string", false, false, false),
                                    new PropertyDef("lead_time_days", "리드타임(일)", "integer", false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Drawing",
                            "부품의 형상, 치수, 공차를 정의하는 기술 문서",
                            List.of(
                                    new PropertyDef("drawing_number", "도면번호", "string", true, true, true),
                                    new PropertyDef("name", "도면명", "string", false, false, false),
                                    new PropertyDef("file_path", "파일 경로", "string", false, false, false),
                                    new PropertyDef("version", "버전", "string", false, false, false),
                                    new PropertyDef("status", "상태", "string", false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Supplier",
                            "부품이나 원자재를 공급하는 외부 업체",
                            List.of(
                                    new PropertyDef("company_name", "회사명", "string", true, true, true),
                                    new PropertyDef("code", "업체 코드", "string", false, true, false),
                                    new PropertyDef("country", "국가", "string", false, false, false),
                                    new PropertyDef("contact_info", "연락처 정보", "string", false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Project",
                            "제품 개발 또는 생산을 위한 프로젝트 단위",
                            List.of(
                                    new PropertyDef("name", "프로젝트명", "string", true, true, true),
                                    new PropertyDef("project_code", "프로젝트 코드", "string", false, true, false),
                                    new PropertyDef("manager", "담당자(PM)", "string", false, false, false),
                                    new PropertyDef("target_date", "목표 완료일", "string", false, false, false),
                                    new PropertyDef("status", "진행 상태", "string", false, false, false)
                            )
                    )
            ),
            List.of(
                    new RelationshipTypeDef(
                            "CONSISTS_OF",
                            "상위 부품이 하위 부품을 포함하는 BOM 관계",
                            "Part",
                            "Part",
                            List.of(
                                    new PropertyDef("quantity", "소요 수량", "integer", false, false, false),
                                    new PropertyDef("sequence", "조립 순서", "integer", false, false, false),
                                    new PropertyDef("reference_designator", "참조 지시자", "string", false, false, false),
                                    new PropertyDef("find_number", "찾기번호", "string", false, false, false)
                            )
                    ),
                    new RelationshipTypeDef(
                            "DEFINED_BY",
                            "부품이 참조하는 도면 관계",
                            "Part",
                            "Drawing",
                            List.of()
                    ),
                    new RelationshipTypeDef(
                            "SUPPLIED_BY",
                            "부품의 공급사 관계",
                            "Part",
                            "Supplier",
                            List.of(
                                    new PropertyDef("unit_cost", "단가", "float", false, false, false)
                            )
                    ),
                    new RelationshipTypeDef(
                            "HAS_ITEM",
                            "프로젝트에 소속된 부품 관계",
                            "Project",
                            "Part",
                            List.of()
                    )
            )
    );

    private ManufacturingOntology() {
    }

    public record OntologyDef(
            String name,
            String description,
            List<NodeLabelDef> nodeLabels,
            List<RelationshipTypeDef> relationshipTypes
    ) {
    }

    public record NodeLabelDef(
            String label,
            String description,
            List<PropertyDef> properties
    ) {
        public List<String> mergeKeys() {
            return properties.stream()
                    .filter(PropertyDef::isMergeKey)
                    .map(PropertyDef::name)
                    .toList();
        }
    }

    public record RelationshipTypeDef(
            String relType,
            String description,
            String fromLabel,
            String toLabel,
            List<PropertyDef> properties
    ) {
    }

    public record PropertyDef(
            String name,
            String description,
            String dataType,
            boolean required,
            boolean isIndexed,
            boolean isMergeKey
    ) {
    }
}
