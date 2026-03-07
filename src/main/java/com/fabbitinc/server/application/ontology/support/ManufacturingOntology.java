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
                                    new PropertyDef("part_number", "품번", PropertyDataType.STRING, true, true, true),
                                    new PropertyDef("name", "부품명", PropertyDataType.STRING, true, false, false),
                                    new PropertyDef("revision", "리비전", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("material", "재질", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("unit", "단위", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("description", "설명", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("category", "분류", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("is_phantom", "팬텀 조립품 여부", PropertyDataType.BOOLEAN, false, false, false),
                                    new PropertyDef("lifecycle_state", "수명주기 상태", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("lead_time_days", "리드타임(일)", PropertyDataType.INTEGER, false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Drawing",
                            "부품의 형상, 치수, 공차를 정의하는 기술 문서",
                            List.of(
                                    new PropertyDef("drawing_number", "도면번호", PropertyDataType.STRING, true, true, true),
                                    new PropertyDef("name", "도면명", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("file_path", "파일 경로", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("version", "버전", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("status", "상태", PropertyDataType.STRING, false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Supplier",
                            "부품이나 원자재를 공급하는 외부 업체",
                            List.of(
                                    new PropertyDef("company_name", "회사명", PropertyDataType.STRING, true, true, true),
                                    new PropertyDef("code", "업체 코드", PropertyDataType.STRING, false, true, false),
                                    new PropertyDef("country", "국가", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("contact_info", "연락처 정보", PropertyDataType.STRING, false, false, false)
                            )
                    ),
                    new NodeLabelDef(
                            "Project",
                            "제품 개발 또는 생산을 위한 프로젝트 단위",
                            List.of(
                                    new PropertyDef("name", "프로젝트명", PropertyDataType.STRING, true, true, true),
                                    new PropertyDef("project_code", "프로젝트 코드", PropertyDataType.STRING, false, true, false),
                                    new PropertyDef("manager", "담당자(PM)", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("target_date", "목표 완료일", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("status", "진행 상태", PropertyDataType.STRING, false, false, false)
                            )
                    )
            ),
            List.of(
                    new RelationshipTypeDef(
                            RelationshipType.CONSISTS_OF,
                            "상위 부품이 하위 부품을 포함하는 BOM 관계",
                            "Part",
                            "Part",
                            List.of(
                                    new PropertyDef("quantity", "소요 수량", PropertyDataType.INTEGER, false, false, false),
                                    new PropertyDef("sequence", "조립 순서", PropertyDataType.INTEGER, false, false, false),
                                    new PropertyDef("reference_designator", "참조 지시자", PropertyDataType.STRING, false, false, false),
                                    new PropertyDef("find_number", "찾기번호", PropertyDataType.STRING, false, false, false)
                            )
                    ),
                    new RelationshipTypeDef(
                            RelationshipType.DEFINED_BY,
                            "부품이 참조하는 도면 관계",
                            "Part",
                            "Drawing",
                            List.of()
                    ),
                    new RelationshipTypeDef(
                            RelationshipType.SUPPLIED_BY,
                            "부품의 공급사 관계",
                            "Part",
                            "Supplier",
                            List.of(
                                    new PropertyDef("unit_cost", "단가", PropertyDataType.FLOAT, false, false, false)
                            )
                    ),
                    new RelationshipTypeDef(
                            RelationshipType.HAS_ITEM,
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
        public NodeLabelDef getNodeLabel(String label) {
            return nodeLabels.stream()
                    .filter(nodeLabelDef -> nodeLabelDef.label().equals(label))
                    .findFirst()
                    .orElse(null);
        }

        public String toMappingPromptText() {
            StringBuilder builder = new StringBuilder();
            builder.append("# ").append(name).append(" - 매핑 가이드").append("\n\n");

            for (NodeLabelDef nodeLabel : nodeLabels) {
                builder.append("## ").append(nodeLabel.label())
                        .append(" (").append(nodeLabel.description()).append(")").append("\n");
                builder.append("속성:").append('\n');
                for (PropertyDef property : nodeLabel.properties()) {
                    String flags = buildPropertyFlags(property);
                    builder.append("  - ").append(property.name())
                            .append(" (").append(property.dataType().value()).append("): ")
                            .append(property.description());
                    if (!flags.isBlank()) {
                        builder.append(" [").append(flags).append("]");
                    }
                    builder.append('\n');
                }
                builder.append('\n');
            }

            builder.append("## 관계 타입").append("\n\n");
            for (RelationshipTypeDef relationshipType : relationshipTypes) {
                NodeLabelDef targetNode = getNodeLabel(relationshipType.toLabel());
                List<String> mergeKeys = targetNode == null ? List.of() : targetNode.mergeKeys();

                builder.append("### ").append(relationshipType.fromLabel())
                        .append(" -[").append(relationshipType.relType().value())
                        .append("]-> ").append(relationshipType.toLabel()).append('\n');
                builder.append("설명: ").append(relationshipType.description()).append('\n');
                builder.append("대상 노드 MERGE KEY: ")
                        .append(mergeKeys.isEmpty() ? "없음" : String.join(", ", mergeKeys))
                        .append('\n');

                if (relationshipType.properties().isEmpty()) {
                    builder.append("관계 속성: 없음").append("\n\n");
                    continue;
                }

                builder.append("관계 속성 (rel_columns 매핑 대상):").append('\n');
                for (PropertyDef property : relationshipType.properties()) {
                    builder.append("  - ").append(property.name())
                            .append(" (").append(property.dataType().value()).append("): ")
                            .append(property.description())
                            .append('\n');
                }
                builder.append('\n');
            }

            return builder.toString();
        }

        private String buildPropertyFlags(PropertyDef property) {
            StringBuilder flags = new StringBuilder();

            if (property.isMergeKey()) {
                flags.append("MERGE KEY - 반드시 매핑 필요");
            }
            if (property.required()) {
                if (!flags.isEmpty()) {
                    flags.append(", ");
                }
                flags.append("필수");
            }

            return flags.toString();
        }
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
            RelationshipType relType,
            String description,
            String fromLabel,
            String toLabel,
            List<PropertyDef> properties
    ) {
    }

    public record PropertyDef(
            String name,
            String description,
            PropertyDataType dataType,
            boolean required,
            boolean isIndexed,
            boolean isMergeKey
    ) {
    }
}
