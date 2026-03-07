package com.fabbitinc.server.application.ontology.support;

import java.util.List;

public final class OntologyMappingPromptRenderer {

    private OntologyMappingPromptRenderer() {
    }

    public static String render(ManufacturingOntology.OntologyDef ontology) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(ontology.name()).append(" - 매핑 가이드").append("\n\n");

        for (ManufacturingOntology.NodeLabelDef nodeLabel : ontology.nodeLabels()) {
            builder.append("## ").append(nodeLabel.label())
                    .append(" (").append(nodeLabel.semanticDescription()).append(")").append("\n");
            builder.append("속성:").append('\n');
            for (ManufacturingOntology.PropertyDef property : nodeLabel.properties()) {
                String flags = buildPropertyFlags(property);
                builder.append("  - ").append(property.name())
                        .append(" (").append(property.dataType().value()).append("): ")
                        .append(property.description());
                if (property.semanticDescription() != null && !property.semanticDescription().isBlank()) {
                    builder.append(". ").append(property.semanticDescription());
                }
                if (!property.examples().isEmpty()) {
                    builder.append(". 예: ").append(String.join(", ", property.examples()));
                }
                if (!property.aliases().isEmpty()) {
                    builder.append(". Excel에서 ").append(String.join(", ", property.aliases()))
                            .append(" 등으로 표기될 수 있음");
                }
                if (!flags.isBlank()) {
                    builder.append(". [").append(flags).append("]");
                }
                builder.append('\n');
            }
            builder.append('\n');
        }

        builder.append("## 관계 타입").append("\n\n");
        for (ManufacturingOntology.RelationshipTypeDef relationshipType : ontology.relationshipTypes()) {
            ManufacturingOntology.NodeLabelDef targetNode = ontology.getNodeLabel(relationshipType.toLabel());
            List<String> mergeKeys = targetNode == null ? List.of() : targetNode.mergeKeys();

            builder.append("### ").append(relationshipType.fromLabel())
                    .append(" -[").append(relationshipType.relType().value())
                    .append("]-> ").append(relationshipType.toLabel()).append('\n');
            builder.append("설명: ").append(relationshipType.semanticDescription()).append('\n');
            builder.append("대상 노드 MERGE KEY: ")
                    .append(mergeKeys.isEmpty() ? "없음" : String.join(", ", mergeKeys))
                    .append('\n');

            if (relationshipType.properties().isEmpty()) {
                builder.append("관계 속성: 없음").append("\n\n");
                continue;
            }

            builder.append("관계 속성 (rel_columns 매핑 대상):").append('\n');
            for (ManufacturingOntology.PropertyDef property : relationshipType.properties()) {
                builder.append("  - ").append(property.name())
                        .append(" (").append(property.dataType().value()).append("): ")
                        .append(property.description());
                if (property.semanticDescription() != null && !property.semanticDescription().isBlank()) {
                    builder.append(". ").append(property.semanticDescription());
                }
                if (!property.examples().isEmpty()) {
                    builder.append(". 예: ").append(String.join(", ", property.examples()));
                }
                if (!property.aliases().isEmpty()) {
                    builder.append(". Excel에서 ").append(String.join(", ", property.aliases()))
                            .append(" 등으로 표기될 수 있음");
                }
                builder.append('\n');
            }
            builder.append('\n');
        }

        return builder.toString();
    }

    private static String buildPropertyFlags(ManufacturingOntology.PropertyDef property) {
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
