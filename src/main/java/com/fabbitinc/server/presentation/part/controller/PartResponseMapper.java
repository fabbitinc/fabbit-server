package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.presentation.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.presentation.part.response.BomChildResponse;
import com.fabbitinc.server.presentation.part.response.BomParentResponse;
import com.fabbitinc.server.presentation.part.response.BomTreeNodeResponse;
import com.fabbitinc.server.presentation.part.response.BomTreeResponse;
import com.fabbitinc.server.presentation.part.response.CategoryLookupResponse;
import com.fabbitinc.server.presentation.part.response.CategoryStatsItemResponse;
import com.fabbitinc.server.presentation.part.response.CategoryStatsResponse;
import com.fabbitinc.server.presentation.part.response.PartAttachmentItemResponse;
import com.fabbitinc.server.presentation.part.response.PartBomResponse;
import com.fabbitinc.server.presentation.part.response.PartDefaultOwnerItemResponse;
import com.fabbitinc.server.presentation.part.response.PartDefaultOwnerListResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartDraftLookupItemResponse;
import com.fabbitinc.server.presentation.part.response.PartDraftLookupResponse;
import com.fabbitinc.server.presentation.part.response.PartFilesResponse;
import com.fabbitinc.server.presentation.part.response.PartFilterOptionsResponse;
import com.fabbitinc.server.presentation.part.response.PartInProgressItemResponse;
import com.fabbitinc.server.presentation.part.response.PartInProgressListResponse;
import com.fabbitinc.server.presentation.part.response.PartListResponse;
import com.fabbitinc.server.presentation.part.response.PartLookupItemResponse;
import com.fabbitinc.server.presentation.part.response.PartLookupResponse;
import com.fabbitinc.server.presentation.part.response.PartOwnerResponse;
import com.fabbitinc.server.presentation.part.response.PartOwnerUserSummaryResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewProcessingResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewResponse;
import com.fabbitinc.server.presentation.part.response.PartProjectSummaryResponse;
import com.fabbitinc.server.presentation.part.response.PartProjectsResponse;
import com.fabbitinc.server.presentation.part.response.PartSummaryResponse;
import com.fabbitinc.server.presentation.part.response.PartSuppliersResponse;
import com.fabbitinc.server.presentation.part.response.RelatedSupplierResponse;
import com.fabbitinc.server.application.part.query.result.BomTreeResult;
import com.fabbitinc.server.application.part.query.result.CategoryLookupResult;
import com.fabbitinc.server.application.part.query.result.CategoryStatsResult;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.application.part.query.result.PartDefaultOwnerListResult;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartDraftLookupResult;
import com.fabbitinc.server.application.part.query.result.PartFilesResult;
import com.fabbitinc.server.application.part.query.result.PartFilterOptionsResult;
import com.fabbitinc.server.application.part.query.result.PartInProgressListResult;
import com.fabbitinc.server.application.part.query.result.PartListResult;
import com.fabbitinc.server.application.part.query.result.PartLookupResult;
import com.fabbitinc.server.application.part.query.result.PartOwnerResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewProcessingResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewResult;
import com.fabbitinc.server.application.part.query.result.PartProjectsResult;
import com.fabbitinc.server.application.part.query.result.PartSuppliersResult;
import com.fabbitinc.server.application.part.query.result.PartUserSummaryResult;
import com.fabbitinc.server.application.part.usecase.result.RegisterPartDrawingResult;

final class PartResponseMapper {

    private PartResponseMapper() {
    }

    static PartLookupResponse toPartLookupResponse(PartLookupResult result) {
        return new PartLookupResponse(
                result.items().stream()
                        .map(item -> new PartLookupItemResponse(item.id(), item.partNumber(), item.name()))
                        .toList()
        );
    }

    static PartDraftLookupResponse toPartDraftLookupResponse(PartDraftLookupResult result) {
        return new PartDraftLookupResponse(
                result.items().stream()
                        .map(item -> new PartDraftLookupItemResponse(
                                item.revisionId(),
                                item.partId(),
                                item.partNumber(),
                                item.baseRevisionCode(),
                                item.draftKey(),
                                item.name(),
                                toPartOwnerUserSummaryResponse(item.createdBy())
                        ))
                        .toList()
        );
    }

    static CategoryStatsResponse toCategoryStatsResponse(CategoryStatsResult result) {
        return new CategoryStatsResponse(
                result.items().stream()
                        .map(item -> new CategoryStatsItemResponse(item.category(), item.partCount()))
                        .toList()
        );
    }

    static CategoryLookupResponse toCategoryLookupResponse(CategoryLookupResult result) {
        return new CategoryLookupResponse(result.items());
    }

    static PartFilterOptionsResponse toPartFilterOptionsResponse(PartFilterOptionsResult result) {
        return new PartFilterOptionsResponse(result.categories(), result.lifecycleStates());
    }

    static PartListResponse toPartListResponse(PartListResult result) {
        return new PartListResponse(
                result.nextCursor(),
                result.prevCursor(),
                result.items().stream()
                        .map(item -> new PartSummaryResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.category(),
                                item.revisionCode(),
                                item.lifecycleState(),
                                item.hasDrawing(),
                                item.childrenCount()
                        ))
                        .toList()
        );
    }

    static PartInProgressListResponse toPartInProgressListResponse(PartInProgressListResult result) {
        return new PartInProgressListResponse(
                result.nextCursor(),
                result.prevCursor(),
                result.items().stream()
                        .map(item -> new PartInProgressItemResponse(
                                item.partId(),
                                item.revisionId(),
                                item.partNumber(),
                                item.name(),
                                item.category(),
                                item.status(),
                                item.revisionCode(),
                                item.draftKey(),
                                item.baseRevisionCode(),
                                item.lifecycleState(),
                                item.hasDrawing(),
                                item.childrenCount(),
                                item.updatedAt()
                        ))
                        .toList()
        );
    }

    static PartDetailResponse toPartDetailResponse(PartDetailResult result) {
        return new PartDetailResponse(
                result.id(),
                result.revisionId(),
                result.partNumber(),
                result.name(),
                result.revision(),
                result.draftKey(),
                result.material(),
                result.unit(),
                result.description(),
                result.category(),
                result.lifecycleState(),
                result.isPhantom(),
                result.leadTimeDays(),
                result.extendedProperties(),
                result.ownerId(),
                toPartOwnerUserSummaryResponse(result.owner()),
                result.ownerTeamId(),
                result.ownerTeamName(),
                toPartPreviewResponse(result.preview()),
                result.draftCount(),
                result.inReviewCount(),
                result.childrenCount(),
                result.parentsCount(),
                result.suppliersCount(),
                result.filesCount(),
                result.projectsCount()
        );
    }

    static PartBomResponse toPartBomResponse(PartBomResult result) {
        return new PartBomResponse(
                result.children().stream()
                        .map(item -> new BomChildResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.revisionCode(),
                                item.lineNumber(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList(),
                result.parents().stream()
                        .map(item -> new BomParentResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.revisionCode(),
                                item.lineNumber(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList()
        );
    }

    static BomTreeResponse toBomTreeResponse(BomTreeResult result) {
        return new BomTreeResponse(
                toBomTreeNodeResponse(result.root()),
                result.direction(),
                result.totalCount()
        );
    }

    private static BomTreeNodeResponse toBomTreeNodeResponse(BomTreeResult.Node node) {
        return new BomTreeNodeResponse(
                node.id(),
                node.partNumber(),
                node.name(),
                node.revision(),
                node.material(),
                node.unit(),
                node.category(),
                node.lifecycleState(),
                node.quantity(),
                node.children().stream().map(PartResponseMapper::toBomTreeNodeResponse).toList()
        );
    }

    static PartProjectsResponse toPartProjectsResponse(PartProjectsResult result) {
        return new PartProjectsResponse(
                result.total(),
                result.items().stream()
                        .map(item -> new PartProjectSummaryResponse(item.id(), item.name(), item.description()))
                        .toList()
        );
    }

    static PartFilesResponse toPartFilesResponse(PartFilesResult result) {
        return new PartFilesResponse(
                result.total(),
                result.items().stream().map(PartResponseMapper::toPartAttachmentItemResponse).toList()
        );
    }

    static PartAttachmentItemResponse toPartAttachmentItemResponse(PartFilesResult.Item item) {
        return new PartAttachmentItemResponse(
                item.attachmentType(),
                item.fileId(),
                item.drawingId(),
                item.originalName(),
                item.contentType(),
                item.fileSize(),
                item.fileUrl(),
                item.previewSelectable(),
                item.selectedAsPreview(),
                item.createdAt()
        );
    }

    static PartSuppliersResponse toPartSuppliersResponse(PartSuppliersResult result) {
        return new PartSuppliersResponse(
                result.total(),
                result.items().stream()
                        .map(item -> new RelatedSupplierResponse(
                                item.id(),
                                item.companyName(),
                                item.code(),
                                item.country(),
                                item.unitCost()
                        ))
                        .toList()
        );
    }

    static PartOwnerResponse toPartOwnerResponse(PartOwnerResult result) {
        return new PartOwnerResponse(
                result.ownerId(),
                toPartOwnerUserSummaryResponse(result.owner()),
                result.ownerTeamId(),
                result.ownerTeamName()
        );
    }

    static PartDefaultOwnerListResponse toPartDefaultOwnerListResponse(PartDefaultOwnerListResult result) {
        return new PartDefaultOwnerListResponse(
                result.items().stream().map(PartResponseMapper::toPartDefaultOwnerItemResponse).toList()
        );
    }

    static PartDefaultOwnerItemResponse toPartDefaultOwnerItemResponse(PartDefaultOwnerListResult.Item result) {
        return new PartDefaultOwnerItemResponse(
                result.id(),
                result.category(),
                result.defaultOwnerId(),
                toPartOwnerUserSummaryResponse(result.defaultOwner()),
                result.defaultOwnerTeamId(),
                result.defaultOwnerTeamName()
        );
    }

    static PartPreviewResponse toPartPreviewResponse(PartPreviewResult result) {
        if (result == null) {
            return null;
        }
        return new PartPreviewResponse(
                result.id(),
                result.sourceType(),
                result.sourceId(),
                result.processingStatus(),
                result.viewerType(),
                result.viewerUrl(),
                result.previewUrl(),
                result.originalFileUrl()
        );
    }

    static PartPreviewProcessingResponse toPartPreviewProcessingResponse(PartPreviewProcessingResult result) {
        return new PartPreviewProcessingResponse(
                result.status(),
                result.failureCode(),
                result.failureMessage(),
                result.pdfReady(),
                result.webpReady(),
                result.glbReady()
        );
    }

    static RegisterDrawingResponse toRegisterDrawingResponse(RegisterPartDrawingResult result) {
        return new RegisterDrawingResponse(
                result.drawingId(),
                result.drawingNumber(),
                result.name()
        );
    }

    private static PartOwnerUserSummaryResponse toPartOwnerUserSummaryResponse(PartUserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new PartOwnerUserSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }
}
