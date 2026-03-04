package com.fabbitinc.server.application.ontology.query;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.ontology.dto.response.NodeLabelSchemaResponse;
import com.fabbitinc.server.application.ontology.dto.response.NodeSearchItemResponse;
import com.fabbitinc.server.application.ontology.dto.response.NodeSearchResponse;
import com.fabbitinc.server.application.ontology.dto.response.OntologySchemaResponse;
import com.fabbitinc.server.application.ontology.dto.response.PropertySchemaResponse;
import com.fabbitinc.server.application.ontology.dto.response.RelationshipTypeSchemaResponse;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OntologyQuery {

    private final AuthTokenParser authTokenParser;
    private final PartRepository partRepository;
    private final DrawingRepository drawingRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;

    private OntologySchemaResponse cachedSchema;

    @Transactional(readOnly = true)
    public OntologySchemaResponse getOntologySchema(String authorizationHeader) {
        authTokenParser.requireAuth(authorizationHeader);
        if (cachedSchema == null) {
            cachedSchema = buildSchema();
        }
        return cachedSchema;
    }

    @Transactional(readOnly = true)
    public NodeSearchResponse searchNodes(
            String authorizationHeader,
            String label,
            String search,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);

        List<NodeSearchItemResponse> items = switch (label) {
            case "Part" -> searchParts(search, limit);
            case "Drawing" -> searchDrawings(search, limit);
            case "Supplier" -> searchSuppliers(search, limit);
            case "Project" -> searchProjects(search, limit);
            default -> throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "지원하지 않는 노드 라벨입니다: " + label
            );
        };

        return new NodeSearchResponse(label, items);
    }

    private OntologySchemaResponse buildSchema() {
        ManufacturingOntology.OntologyDef ontology = ManufacturingOntology.ONTOLOGY;

        List<NodeLabelSchemaResponse> nodeLabels = ontology.nodeLabels().stream()
                .map(node -> new NodeLabelSchemaResponse(
                        node.label(),
                        node.description(),
                        node.properties().stream()
                                .map(this::toPropertySchemaResponse)
                                .toList(),
                        node.mergeKeys()
                ))
                .toList();

        List<RelationshipTypeSchemaResponse> relationshipTypes = ontology.relationshipTypes().stream()
                .map(relationship -> new RelationshipTypeSchemaResponse(
                        relationship.relType(),
                        relationship.description(),
                        relationship.fromLabel(),
                        relationship.toLabel(),
                        relationship.properties().stream()
                                .map(this::toPropertySchemaResponse)
                                .toList()
                ))
                .toList();

        return new OntologySchemaResponse(
                ontology.name(),
                ontology.description(),
                nodeLabels,
                relationshipTypes
        );
    }

    private PropertySchemaResponse toPropertySchemaResponse(ManufacturingOntology.PropertyDef property) {
        return new PropertySchemaResponse(
                property.name(),
                property.description(),
                property.dataType(),
                property.required(),
                property.isMergeKey()
        );
    }

    private List<NodeSearchItemResponse> searchParts(String search, int limit) {
        return partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                        search,
                        search,
                        PageRequest.of(0, limit)
                ).stream()
                .map(part -> new NodeSearchItemResponse(part.getPartNumber(), part.getName()))
                .toList();
    }

    private List<NodeSearchItemResponse> searchDrawings(String search, int limit) {
        return drawingRepository.findByDrawingNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByDrawingNumberAsc(
                        search,
                        search,
                        PageRequest.of(0, limit)
                ).stream()
                .map(drawing -> new NodeSearchItemResponse(drawing.getDrawingNumber(), drawing.getName()))
                .toList();
    }

    private List<NodeSearchItemResponse> searchSuppliers(String search, int limit) {
        return supplierRepository.listSuppliersPaginated(search, 0, limit).stream()
                .map(supplier -> new NodeSearchItemResponse(
                        supplier.getCompanyName(),
                        supplier.getCompanyName()
                ))
                .toList();
    }

    private List<NodeSearchItemResponse> searchProjects(String search, int limit) {
        return projectRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                        search,
                        PageRequest.of(0, limit)
                ).stream()
                .map(project -> new NodeSearchItemResponse(project.getName(), project.getName()))
                .toList();
    }
}
