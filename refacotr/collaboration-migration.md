# 협업 도메인 마이그레이션 추적

## 범위

- 레거시 FastAPI 서비스:
  - `../server/app/modules/project/service.py`
  - `../server/app/modules/team/service.py`
  - `../server/app/modules/issue/service.py`
  - `../server/app/modules/label/service.py`
  - `../server/app/modules/notification/service.py`
- 레거시 유스케이스:
  - `../server/app/use_cases/project/*`
  - `../server/app/use_cases/team/*`
  - `../server/app/use_cases/issue/*`
  - `../server/app/use_cases/label/*`
  - `../server/app/use_cases/notification/*`
- 현재 Spring Boot:
  - `src/main/java/com/fabbitinc/server/application/project/**`
  - `src/main/java/com/fabbitinc/server/application/team/**`
  - `src/main/java/com/fabbitinc/server/application/issue/**`
  - `src/main/java/com/fabbitinc/server/application/label/**`
  - `src/main/java/com/fabbitinc/server/application/notification/**`
  - 관련 `presentation/**`, `domain/**`, `infrastructure/**`

## 요약

- 레거시 `usecase.py` 파일은 없었습니다. 실제 유스케이스 레이어는 `../server/app/use_cases/...` 디렉터리였습니다.
- 협업 도메인 핵심 쓰기 기능은 대부분 Spring으로 이관되었습니다. 특히 `team`, `issue/change request`, `label`, `notification read`는 동작 경로가 거의 대응됩니다.
- 큰 갭은 2개입니다.
  - 프로젝트 활동 피드 쓰기 경로가 비어 있습니다. 조회 API는 있지만 레거시의 `ProjectUpdated`/`ProjectPartsLinked`/`ProjectPartsUnlinked`에 대응하는 activity 기록 로직이 없습니다.
  - 알림 SSE 스트림은 연결만 되고 실제 push 발행자가 없습니다. 레거시는 `UserMentioned` 후 커밋 뒤 워커가 SSE를 밀어줬지만, 현재는 `SseManager.push(...)` 호출 지점이 없습니다.

## 프로젝트 도메인

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `project.service.get_or_raise` | `ProjectService.getOrThrow` | 완료 | 소프트 삭제 제외 조회와 `NOT_FOUND` 매핑이 대응됩니다. |
| `project.service.ensure_project_active` | `Project.ensureActive`, `ProjectService.updateProject/linkParts` | 완료 | 활성 상태 제약은 도메인 메서드와 서비스에서 유지됩니다. |
| `project.service.ensure_project_admin` | `ProjectService.ensureProjectAdmin` | 완료 | `ADMIN` 멤버 검증이 동일합니다. |
| `project.service.archive_project` + `use_cases.project.archive_project` | `ArchiveProjectUseCase.execute` -> `ProjectService.archiveProject` | 부분 | 보관 동작은 이관됐지만 레거시의 프로젝트 activity 기록 체인이 없습니다. 이미 보관된 경우 에러 코드도 `PROJECT_ARCHIVED`에서 `INVALID_STATE` 계열로 바뀝니다. |
| `project.service.unarchive_project` + `use_cases.project.archive_project.unarchive_project` | `UnarchiveProjectUseCase.execute` -> `ProjectService.unarchiveProject` | 부분 | 복원 동작은 있으나 프로젝트 activity 기록이 없습니다. 레거시는 비보관 상태 복원 시 `BAD_REQUEST`, 현재는 `INVALID_STATE`입니다. |
| `project.service.delete_project` + `use_cases.project.delete_project` | `DeleteProjectUseCase.execute` -> `ProjectService.deleteProject` | 완료 | ADMIN 검증 후 소프트 삭제 흐름이 유지됩니다. |
| `project.service.create_project` + `use_cases.project.create_project` | `CreateProjectUseCase.execute` -> `ProjectService.createProject` | 완료 | 프로젝트 생성과 생성자 `ADMIN` 멤버 자동 등록이 대응됩니다. |
| `project.service.update_project` + `use_cases.project.update_project` | `UpdateProjectUseCase.execute` -> `ProjectService.updateProject` | 부분 | 이름/설명 변경 자체는 이관됐지만 레거시의 `ProjectUpdated` 이벤트 기반 activity 기록이 현재 없습니다. |
| `project.service.link_parts` + `use_cases.project.link_parts` | `LinkProjectPartsUseCase.execute` -> `ProjectService.linkParts` | 부분 | part 존재 검증과 중복 제거는 이관됐지만 레거시의 `ProjectPartsLinked` activity 기록이 없습니다. |
| `project.service.unlink_parts` + `use_cases.project.unlink_parts` | `UnlinkProjectPartsUseCase.execute` -> `ProjectService.unlinkParts` | 부분 | unlink 자체는 이관됐지만 레거시의 `ProjectPartsUnlinked` activity 기록이 없습니다. |
| `project.service.validate_parts_in_project` | 직접 대응 없음 | 확인필요 | 레거시에서도 호출 흔적이 없었습니다. 현재 명시 helper는 없지만 실제 기능 누락인지 dead code인지 추가 확인이 필요합니다. |
| `project.service.add_members` + `use_cases.project.manage_members.add_members` | `AddProjectMembersUseCase.execute` -> `ProjectService.addMembers` | 완료 | 멤버 배치 추가와 역할 기본값(`MEMBER`) 처리가 대응됩니다. |
| `project.service.remove_members` + `use_cases.project.manage_members.remove_members` | `RemoveProjectMembersUseCase.execute` -> `ProjectService.removeMembers` | 완료 | 멤버 배치 제거가 대응됩니다. |

## 팀 도메인

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `team.service.get_or_raise` | `TeamService.getOrThrow` | 완료 | `NOT_FOUND` 조회가 동일합니다. |
| `team.service.create_team` + `use_cases.team.create_team` | `CreateTeamUseCase.execute` -> `TeamService.createTeam` | 완료 | `created_by` 저장과 팀 생성 흐름이 대응됩니다. |
| `team.service.update_team` + `use_cases.team.update_team` | `UpdateTeamUseCase.execute` -> `TeamService.updateTeam` | 완료 | 이름/설명 부분 수정이 유지됩니다. |
| `team.service.delete_team` + `use_cases.team.delete_team` | `DeleteTeamUseCase.execute` -> `TeamService.deleteTeam` | 완료 | 팀 삭제 흐름이 대응됩니다. |
| `team.service.add_members` + `use_cases.team.manage_members.add_members` | `AddTeamMembersUseCase.execute` -> `TeamService.addMembers` | 완료 | 중복 제거 후 배치 추가가 동일합니다. |
| `team.service.remove_members` + `use_cases.team.manage_members.remove_members` | `RemoveTeamMembersUseCase.execute` -> `TeamService.removeMembers` | 완료 | 배치 제거가 동일합니다. |

## 이슈 / 변경요청 도메인

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `issue.service._register_mention_events` | `IssueService.registerMentions` | 부분 | 이슈 멘션 activity와 사용자 멘션 notification 생성은 이관됐습니다. 다만 레거시는 커밋 후 워커가 SSE push까지 수행했지만 현재는 `SseManager.push(...)` 호출자가 없어 실시간 푸시는 빠져 있습니다. |
| `issue.service.get_or_raise` | `IssueService.getIssueOrThrow` | 완료 | ID 기반 조회와 `NOT_FOUND` 매핑이 유지됩니다. |
| `issue.service.get_issue_by_number_or_raise` | `IssueService.getIssueByNumberOrThrow` | 완료 | 번호 기반 ISSUE 조회가 유지됩니다. |
| `issue.service.create_issue` + `use_cases.issue.create_issue` | `CreateIssueUseCase.execute` -> `IssueService.createIssue` | 완료 | 채번, 본문 검증, 멘션 등록, 부품/담당자/라벨/파일 일괄 연결 흐름이 대응됩니다. |
| `issue.service.create_change_request` + `use_cases.issue.create_change_request` | `CreateChangeRequestUseCase.execute` -> `IssueService.createChangeRequest` | 완료 | CR 생성, 이슈 연결, 검토자/팀검토자 포함 초기 연관 데이터 일괄 연결이 대응됩니다. |
| `issue.service.ensure_issue_editable` | `IssueService.updateIssue` 내부 상태 검증 | 완료 | 닫힌 이슈 수정 금지가 동일하게 유지됩니다. |
| `issue.service.update_issue` + `use_cases.issue.update_issue` | `UpdateIssueUseCase.execute` -> `IssueService.updateIssue` | 완료 | 제목/본문 수정과 멘션 diff 처리, 닫힌 이슈 수정 금지가 유지됩니다. |
| `issue.service.attach_files` + `use_cases.issue.add_files` | `AddIssueFilesUseCase.execute` -> `IssueService.attachFiles` | 완료 | attachable 검증 후 파일 owner를 issue로 연결하는 흐름이 대응됩니다. |
| `issue.service.detach_file` + `use_cases.issue.delete_file` | `DeleteIssueFileUseCase.execute` -> `IssueService.detachFile` | 완료 | 이슈 소유 파일 검증 후 soft delete와 activity 기록이 대응됩니다. |
| `issue.service.sync_assignees` + `use_cases.issue.sync_assignees` | `SyncAssigneesUseCase.execute` -> `IssueService.syncAssignees` | 완료 | diff 계산, 팀 배정 중복 제외, activity 기록이 대응됩니다. |
| `issue.service.sync_reviewers` + `use_cases.issue.sync_reviewers` | `SyncReviewersUseCase.execute` -> `IssueService.syncReviewers` | 완료 | diff 계산, 팀 검토자 중복 제외, activity 기록이 대응됩니다. |
| `issue.service.sync_labels` + `use_cases.issue.sync_labels` | `SyncLabelsUseCase.execute` -> `IssueService.syncLabels` | 완료 | 라벨 존재 검증, diff 동기화, activity 기록이 대응됩니다. |
| `issue.service.sync_parts` + `use_cases.issue.sync_parts` | `SyncPartsUseCase.execute` -> `IssueService.syncParts` | 완료 | diff 동기화와 activity 기록이 대응됩니다. |
| `issue.service.get_cr_or_raise` | `IssueService.getChangeRequestOrThrow`, `getChangeRequestByNumberOrThrow` | 완료 | CR 조회가 ID/번호 양쪽으로 대응됩니다. |
| `issue.service.sync_issues` + `use_cases.issue.sync_issues` | `SyncIssuesUseCase.execute` -> `IssueService.syncIssues` | 완료 | CR-Issue 양방향 activity까지 포함해 대응됩니다. |
| `issue.service.sync_changes` + `use_cases.issue.sync_changes` | `SyncChangesUseCase.execute` -> `IssueService.syncChanges` | 완료 | Issue-CR 역방향 연결과 양방향 activity가 대응됩니다. |
| `issue.service.close_linked_open_issues` | `IssueService.closeLinkedOpenIssuesIfResolved` | 완료 | CR merge 후 unresolved CR이 없는 linked issue 자동 close가 유지됩니다. |
| `issue.service.close_issue` + `use_cases.issue.close_issue` | `CloseIssueUseCase.execute` -> `IssueService.closeIssue` | 완료 | 상태 전이와 timeline activity 기록이 대응됩니다. |
| `issue.service.reopen_issue` + `use_cases.issue.reopen_issue` | `ReopenIssueUseCase.execute` -> `IssueService.reopenIssue` | 완료 | reopen 상태 전이와 activity 기록이 대응됩니다. |
| `issue.service.update_cr` + `use_cases.issue.update_change_request` | `UpdateChangeRequestUseCase.execute` -> `IssueService.updateChangeRequest` | 완료 | MERGED/CLOSED 수정 금지와 본문/제목 수정이 대응됩니다. |
| `issue.service.submit_cr` + `use_cases.issue.submit_cr` | `SubmitChangeRequestUseCase.execute` -> `IssueService.submitChangeRequest` | 완료 | `DRAFT -> SUBMITTED` 전이가 대응됩니다. |
| `issue.service.merge_cr` + `use_cases.issue.merge_cr` | `MergeChangeRequestUseCase.execute` -> `IssueService.mergeChangeRequest` | 완료 | `SUBMITTED -> MERGED`, merge 시각/행위자 기록, linked issue 자동 close가 대응됩니다. |
| `issue.service.close_cr` + `use_cases.issue.close_cr` | `CloseChangeRequestUseCase.execute` -> `IssueService.closeChangeRequest` | 완료 | close 전이가 대응됩니다. |
| `issue.service.reopen_cr` + `use_cases.issue.reopen_cr` | `ReopenChangeRequestUseCase.execute` -> `IssueService.reopenChangeRequest` | 완료 | `CLOSED -> SUBMITTED` 재개 전이가 대응됩니다. |
| `issue.service.get_comment_or_raise` | `IssueService` 내부 comment 조회 | 완료 | `NOT_FOUND` 조회가 동일합니다. |
| `issue.service.create_comment` + `use_cases.issue.create_comment` | `CreateCommentUseCase.execute` -> `IssueService.createComment` | 완료 | 댓글 생성과 멘션 처리, timeline 조회 대응이 유지됩니다. |
| `issue.service.update_comment` + `use_cases.issue.update_comment` | `UpdateCommentUseCase.execute` -> `IssueService.updateComment` | 완료 | 본인 댓글만 수정 가능, 멘션 diff 처리까지 대응됩니다. |
| `issue.service.delete_comment` + `use_cases.issue.delete_comment` | `DeleteCommentUseCase.execute` -> `IssueService.deleteComment` | 완료 | 본인 댓글만 삭제 가능 제약이 유지됩니다. |
| `issue.service.sync_team_assignees` + `use_cases.issue.sync_team_assignees` | `SyncTeamAssigneesUseCase.execute` -> `IssueService.syncTeamAssignees` | 완료 | 팀 배정 diff와 중복 개인 assignee 자동 제거가 대응됩니다. |
| `issue.service.sync_team_reviewers` + `use_cases.issue.sync_team_reviewers` | `SyncTeamReviewersUseCase.execute` -> `IssueService.syncTeamReviewers` | 완료 | 팀 검토자 diff와 중복 개인 reviewer 자동 제거가 대응됩니다. |
| `issue.service.submit_review` + `use_cases.issue.submit_review` | `SubmitReviewUseCase.execute` -> `IssueService.submitReview` | 완료 | 본인 reviewer 상태 업데이트와 reviewedAt 기록이 대응됩니다. 현재 구현은 `PENDING`을 명시적으로 거부해 오히려 더 엄격합니다. |

## 라벨 도메인

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `label.service.get_or_raise` | `LabelService.getOrThrow` | 완료 | `NOT_FOUND` 조회가 대응됩니다. |
| `label.service.create_label` + `use_cases.label.create_label` | `CreateLabelUseCase.execute` -> `LabelService.createLabel` | 완료 | 이름 중복 검사와 생성이 대응됩니다. |
| `label.service.update_label` + `use_cases.label.update_label` | `UpdateLabelUseCase.execute` -> `LabelService.updateLabel` | 완료 | 이름 중복 검사, description unset, color 변경이 대응됩니다. |
| `label.service.delete_label` + `use_cases.label.delete_label` | `DeleteLabelUseCase.execute` -> `LabelService.deleteLabel` | 완료 | 삭제 흐름이 대응됩니다. |
| `label.service.seed_defaults` | `TenantProvisioningAdapter.seedDefaultLabels` | 완료 | 기본 라벨 시딩은 `LabelService`가 아니라 테넌트 프로비저닝 단계로 이동했습니다. 사용자 기능 관점의 시딩 자체는 유지됩니다. |

## 알림 도메인

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `notification.service.mark_as_read` + `use_cases.notification.mark_as_read` | `MarkNotificationReadUseCase.execute` -> `NotificationService.markAsRead` | 완료 | 본인 알림 단건 읽음 처리와 `NOT_FOUND` 매핑이 대응됩니다. |
| `notification.service.mark_all_as_read` + `use_cases.notification.mark_all_as_read` | `MarkAllNotificationsReadUseCase.execute` -> `NotificationService.markAllAsRead` | 완료 | 본인 미읽음 전체 읽음 처리 흐름이 대응됩니다. |

## 핵심 갭

- 프로젝트 activity 피드 미완성
  - 레거시는 `ProjectUpdated`, `ProjectPartsLinked`, `ProjectPartsUnlinked` 이벤트를 `app/modules/activity/handlers.py`가 activity row로 적재했습니다.
  - 현재는 `ProjectController`에 `GET /api/v1/projects/{projectId}/activities`가 있고 `ProjectQuery.listActivities(...)`도 존재하지만, `ProjectService` 어느 경로에서도 `Activity`를 기록하지 않습니다.
  - `ActivityAction` enum에도 프로젝트 action이 없습니다. 현재 상태로는 프로젝트 activity 조회가 비어 있거나 미래에 데이터가 들어와도 해석할 수 없습니다.

- 알림 SSE 실시간 push 미구현
  - 레거시는 `UserMentioned`를 커밋 후 워커가 처리하면서 Notification row 생성과 `sse_manager.push(...)`를 함께 수행했습니다.
  - 현재는 `IssueService.registerMentions(...)`가 Notification row 저장만 수행하고, `NotificationStreamUseCase`는 연결만 관리합니다.
  - `src/main/java/com/fabbitinc/server/application/notification/support/SseManager.java`에 `push(...)`는 있지만 호출처가 없습니다.

## 리스크

- 알림 생성이 이제 이슈/댓글 트랜잭션 안에서 동기 저장됩니다. 레거시는 알림 실패가 원 요청을 롤백시키지 않았지만, 현재는 notification 저장 실패가 상위 유스케이스를 실패시킬 수 있습니다.
- 프로젝트 보관/복원 실패 시 에러 코드 의미가 일부 달라졌습니다. 클라이언트가 `PROJECT_ARCHIVED` 또는 `BAD_REQUEST`를 전제로 처리했다면 회귀 가능성이 있습니다.
- `project.service.validate_parts_in_project`는 레거시에서 호출 흔적이 없어서 우선순위는 낮지만, 숨은 호출 경로가 있었다면 현재 명시 검증 함수가 없습니다.
