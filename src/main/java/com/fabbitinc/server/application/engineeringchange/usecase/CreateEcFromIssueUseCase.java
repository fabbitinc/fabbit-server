package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.usecase.command.CreateEcFromIssueCommand;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssuePart;
import com.fabbitinc.server.domain.issue.repository.IssuePartRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 이슈로부터 설계변경(EC)을 생성하는 유스케이스.
 * 이슈에 연결된 부품의 DRAFT 리비전을 영향 항목으로 자동 등록하고,
 * 영향 분석 결과를 본문에 요약하여 설계변경을 생성한다.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class CreateEcFromIssueUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueRepository issueRepository;
    private final IssuePartRepository issuePartRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final CreateEngineeringChangeUseCase createEngineeringChangeUseCase;
    private final ObjectMapper objectMapper;

    public CreateEngineeringChangeUseCase.CreateEngineeringChangeResult execute(CreateEcFromIssueCommand command) {
        currentAuthProvider.getCurrentAuth();

        // 1. 이슈 조회
        Issue issue = issueRepository.findById(command.issueId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Issue '" + command.issueId() + "'을(를) 찾을 수 없습니다"
                ));

        // 2. 제목 결정
        String title = command.title() != null ? command.title() : issue.getTitle();

        // 3. 연결된 부품 ID 조회
        List<UUID> linkedPartIds = issuePartRepository.findByIssueId(command.issueId()).stream()
                .map(IssuePart::getPartId)
                .toList();

        // 4. 각 부품의 최신 DRAFT 리비전 → 영향 항목 생성
        List<CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.AffectedItemTarget> affectedItems =
                buildAffectedItems(linkedPartIds);

        // 5. 간단 요약 본문 생성
        JsonNode body = command.body() != null ? command.body() : buildImpactSummaryBody(linkedPartIds, affectedItems.size());

        // 6. 단계(steps) 구성: 리뷰어 → 승인자 순서
        List<CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget> steps =
                buildSteps(command.reviewerIds(), command.approverIds());

        // 7. EC 생성 위임
        return createEngineeringChangeUseCase.execute(
                new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand(
                        title,
                        body,
                        command.issueId(),
                        affectedItems,
                        List.of(),
                        steps
                )
        );
    }

    private List<CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.AffectedItemTarget> buildAffectedItems(
            List<UUID> partIds
    ) {
        if (partIds.isEmpty()) {
            return List.of();
        }

        // 부품별 최신 DRAFT 리비전을 찾아 영향 항목으로 등록
        List<PartRevision> revisions = partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(partIds);
        Map<UUID, PartRevision> latestDraftByPartId = new LinkedHashMap<>();
        for (PartRevision revision : revisions) {
            if (revision.getStatus() == PartRevisionStatus.DRAFT
                    && !latestDraftByPartId.containsKey(revision.getPartId())) {
                latestDraftByPartId.put(revision.getPartId(), revision);
            }
        }

        return latestDraftByPartId.values().stream()
                .map(revision -> new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.AffectedItemTarget(
                        EngineeringChangeAffectedItemType.REVISION_RELEASE,
                        revision.getId(),
                        null
                ))
                .toList();
    }

    private JsonNode buildImpactSummaryBody(List<UUID> partIds, int draftRevisionCount) {
        String summaryText = String.format(
                "EC 생성 요약: 연결 부품 %d건, 영향 항목으로 등록된 DRAFT 리비전 %d건",
                partIds.size(), draftRevisionCount
        );

        // TipTap 형식 paragraph 노드로 구성
        String tiptapJson = """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}"""
                .formatted(summaryText);

        return objectMapper.readTree(tiptapJson);
    }

    private List<CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget> buildSteps(
            List<UUID> reviewerIds,
            List<UUID> approverIds
    ) {
        List<CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget> steps = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(1);

        for (UUID reviewerId : reviewerIds) {
            steps.add(new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget(
                    EngineeringChangeStepType.REVIEW,
                    EngineeringChangeStepAssigneeType.USER,
                    reviewerId,
                    sequence.getAndIncrement()
            ));
        }

        for (UUID approverId : approverIds) {
            steps.add(new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget(
                    EngineeringChangeStepType.APPROVAL,
                    EngineeringChangeStepAssigneeType.USER,
                    approverId,
                    sequence.getAndIncrement()
            ));
        }

        return steps;
    }
}
