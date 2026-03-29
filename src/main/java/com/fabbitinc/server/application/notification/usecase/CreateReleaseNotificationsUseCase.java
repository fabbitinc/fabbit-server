package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.notification.event.NotificationCreatedEvent;
import com.fabbitinc.server.application.notification.service.NotificationService;
import com.fabbitinc.server.application.notification.usecase.command.CreateReleaseNotificationsCommand;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.user.model.User;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateReleaseNotificationsUseCase {

    private static final int MAX_RECIPIENTS = 50;

    private final EngineeringChangeAffectedItemRepository engineeringChangeAffectedItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final IssueApi issueApi;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueLinkRepository;
    private final UserApi userApi;
    private final ObjectMapper objectMapper;

    public void execute(CreateReleaseNotificationsCommand command) {
        // 1. 영향 항목 조회
        List<EngineeringChangeAffectedItem> affectedItems =
                engineeringChangeAffectedItemRepository.findByEngineeringChangeIdAndItemTypeOrderByCreatedAtAsc(
                        command.engineeringChangeId(), EngineeringChangeAffectedItemType.REVISION_RELEASE);

        if (affectedItems.isEmpty()) {
            return;
        }

        // 2. REVISION_RELEASE 항목의 targetId(= revisionId) 기반 partId 수집
        Set<UUID> revisionIds = affectedItems.stream()
                .map(EngineeringChangeAffectedItem::getTargetId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<PartRevision> affectedRevisions = partRevisionRepository.findAllById(revisionIds);
        Set<UUID> affectedPartIds = affectedRevisions.stream()
                .map(PartRevision::getPartId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. 연결된 이슈의 담당자 수집
        Set<UUID> recipientIds = new LinkedHashSet<>();

        List<EngineeringChangeIssueLink> issueLinks =
                engineeringChangeIssueLinkRepository.findByEngineeringChangeId(command.engineeringChangeId());
        for (EngineeringChangeIssueLink link : issueLinks) {
            recipientIds.addAll(issueApi.getIssueAssigneeUserIds(link.getIssueId()));
        }

        // 4. 직접 상위 BOM (1단계) 의 부모 파트 소유자 수집
        Set<UUID> parentRevisionIds = new LinkedHashSet<>();
        for (UUID revisionId : revisionIds) {
            engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(revisionId)
                    .forEach(bomItem -> parentRevisionIds.add(bomItem.getParentPartRevisionId()));
        }

        if (!parentRevisionIds.isEmpty()) {
            List<PartRevision> parentRevisions = partRevisionRepository.findAllById(parentRevisionIds);
            Set<UUID> parentPartIds = parentRevisions.stream()
                    .map(PartRevision::getPartId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // 부모 파트에 연결된 이슈의 담당자 수집
            Set<UUID> parentIssueIds = issueApi.getIssueIdsByPartIds(parentPartIds);
            for (UUID issueId : parentIssueIds) {
                recipientIds.addAll(issueApi.getIssueAssigneeUserIds(issueId));
            }
        }

        // 영향받는 파트에 연결된 이슈의 담당자 수집
        Set<UUID> affectedIssueIds = issueApi.getIssueIdsByPartIds(affectedPartIds);
        for (UUID issueId : affectedIssueIds) {
            recipientIds.addAll(issueApi.getIssueAssigneeUserIds(issueId));
        }

        // 5. 발행자 제외 및 최대 수 제한
        recipientIds.remove(command.actorId());
        List<UUID> finalRecipients = recipientIds.stream()
                .limit(MAX_RECIPIENTS)
                .toList();

        if (finalRecipients.isEmpty()) {
            return;
        }

        // 6. 알림 생성
        User actor = userApi.getUserOrNull(command.actorId());
        for (UUID userId : finalRecipients) {
            Notification notification = notificationService.create(
                    userId,
                    NotificationType.RELEASE,
                    command.actorId(),
                    toReleasePayload(command)
            );
            applicationEventPublisher.publishEvent(
                    NotificationCreatedEvent.create(
                            notification.getId(),
                            notification.getUserId(),
                            command.actorId(),
                            actor == null ? null : actor.getFullName(),
                            actor == null ? null : actor.getProfileImageFileKey(),
                            null,
                            command.ecNumber(),
                            command.ecTitle(),
                            "ENGINEERING_CHANGE",
                            false
                    )
            );
        }
    }

    private String toReleasePayload(CreateReleaseNotificationsCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("engineering_change_id", command.engineeringChangeId().toString());
        payload.put("ec_number", command.ecNumber());
        payload.put("ec_title", command.ecTitle());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 payload 직렬화에 실패했습니다");
        }
    }
}
