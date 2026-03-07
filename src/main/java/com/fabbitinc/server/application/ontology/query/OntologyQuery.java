package com.fabbitinc.server.application.ontology.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.ontology.query.condition.NodeSearchCondition;
import com.fabbitinc.server.application.ontology.query.result.NodeSearchResult;
import com.fabbitinc.server.application.ontology.query.result.OntologySchemaResult;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OntologyQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRepository partRepository;
    private final DrawingRepository drawingRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;

    private OntologySchemaResult cachedSchema;

    public OntologySchemaResult getSchema() {
        currentAuthProvider.getCurrentAuth();
        if (cachedSchema == null) {
            cachedSchema = buildSchema();
        }
        return cachedSchema;
    }

    public NodeSearchResult search(NodeSearchCondition condition) {
        currentAuthProvider.getCurrentAuth();
        String label = condition.label();
        String search = condition.search();
        int limit = condition.limit();

        List<NodeSearchResult.NodeSearchItemResult> items = switch (label) {
            case "Part" -> searchParts(search, limit);
            case "Drawing" -> searchDrawings(search, limit);
            case "Supplier" -> searchSuppliers(search, limit);
            case "Project" -> searchProjects(search, limit);
            default -> throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "지원하지 않는 노드 라벨입니다: " + label
            );
        };

        return new NodeSearchResult(label, items);
    }

    private OntologySchemaResult buildSchema() {
        ManufacturingOntology.OntologyDef ontology = ManufacturingOntology.ONTOLOGY;

        List<OntologySchemaResult.NodeLabelResult> nodeLabels = ontology.nodeLabels().stream()
                .map(node -> new OntologySchemaResult.NodeLabelResult(
                        node.label(),
                        node.description(),
                        node.properties().stream()
                                .map(this::toPropertyResult)
                                .toList(),
                        node.mergeKeys()
                ))
                .toList();

        List<OntologySchemaResult.RelationshipTypeResult> relationshipTypes = ontology.relationshipTypes().stream()
                .map(relationship -> new OntologySchemaResult.RelationshipTypeResult(
                        relationship.relType(),
                        relationship.description(),
                        relationship.fromLabel(),
                        relationship.toLabel(),
                        relationship.properties().stream()
                                .map(this::toPropertyResult)
                                .toList()
                ))
                .toList();

        return new OntologySchemaResult(
                ontology.name(),
                ontology.description(),
                nodeLabels,
                relationshipTypes
        );
    }

    private OntologySchemaResult.PropertyResult toPropertyResult(ManufacturingOntology.PropertyDef property) {
        return new OntologySchemaResult.PropertyResult(
                property.name(),
                property.description(),
                property.dataType(),
                property.required(),
                property.isMergeKey()
        );
    }

    private List<NodeSearchResult.NodeSearchItemResult> searchParts(String search, int limit) {
        return partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                        search,
                        search,
                        PageRequest.of(0, limit)
                ).stream()
                .map(part -> new NodeSearchResult.NodeSearchItemResult(part.getPartNumber(), part.getName()))
                .toList();
    }

    private List<NodeSearchResult.NodeSearchItemResult> searchDrawings(String search, int limit) {
        return drawingRepository.findByDrawingNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByDrawingNumberAsc(
                        search,
                        search,
                        PageRequest.of(0, limit)
                ).stream()
                .map(drawing -> new NodeSearchResult.NodeSearchItemResult(drawing.getDrawingNumber(), drawing.getName()))
                .toList();
    }

    private List<NodeSearchResult.NodeSearchItemResult> searchSuppliers(String search, int limit) {
        if (search == null || search.isBlank()) {
            return supplierRepository.findAllByOrderByCompanyNameAsc(PageRequest.of(0, limit)).stream()
                    .map(supplier -> new NodeSearchResult.NodeSearchItemResult(
                            supplier.getCompanyName(),
                            supplier.getCompanyName()
                    ))
                    .toList();
        }
        return supplierRepository.findByCompanyNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByCompanyNameAsc(
                        search.trim(),
                        search.trim(),
                        PageRequest.of(0, limit)
                ).stream()
                .map(supplier -> new NodeSearchResult.NodeSearchItemResult(
                        supplier.getCompanyName(),
                        supplier.getCompanyName()
                ))
                .toList();
    }

    private List<NodeSearchResult.NodeSearchItemResult> searchProjects(String search, int limit) {
        if (search == null || search.isBlank()) {
            return projectRepository.findByDeletedFalseOrderByNameAsc(PageRequest.of(0, limit)).stream()
                    .map(project -> new NodeSearchResult.NodeSearchItemResult(project.getName(), project.getName()))
                    .toList();
        }
        return projectRepository.findByDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(
                        search.trim(),
                        PageRequest.of(0, limit)
                ).stream()
                .map(project -> new NodeSearchResult.NodeSearchItemResult(project.getName(), project.getName()))
                .toList();
    }
}
