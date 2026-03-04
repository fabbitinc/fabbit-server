# FastAPI -> Spring Boot 마이그레이션 요약 (2026-03-05)

## 1. 수행 내용

- 전체 API 이관 완료
  - 원본 `../server/app/api/v1` 라우터 기준 `path + method` 전수 대조 후 Spring 컨트롤러 1:1 정합성 확보
  - 이슈/변경요청/매핑 포함 모든 도메인 컨트롤러가 실제 UseCase/Query 로직에 연결됨

- `mapping` 도메인 실제 이식 완료
  - `preview / confirm / validate / list / get / update / delete` 구현
  - 파일 파싱(xlsx/xls/csv), 헤더 기반 매핑 생성, 정규화, 검증, 리비전 생성/조회/비활성화 반영
  - DTO 기반 요청/응답으로 정리, Swagger `tag/summary/description` 작성

- `issue`/`change` 도메인 실제 이식 완료
  - 조회: lookup/list/detail/timeline
  - 생성/수정/상태전이: create/update/close/reopen/submit/merge
  - 동기화: assignees/team-assignees/reviewers/team-reviewers/labels/parts/issues/changes
  - 리뷰/댓글/파일: submit-review/create-comment/update-comment/delete-comment/add-files/delete-file
  - TipTap 검증, 멘션 추출, activity 기록, notification 생성, linked issue 자동 close(merge 시) 반영

- 아키텍처 규칙 보정
  - Controller가 domain model 패키지를 직접 참조하지 않도록 `IssueTargetType`(application) 도입
  - Domain(`ChangeRequest`)에서 application 예외 의존 제거
  - 상태 전이 예외는 domain 전용 예외 -> application에서 `AppException(INVALID_STATE)`로 변환

- Spring Boot 4(Jackson 3) 환경 정합화
  - `com.fasterxml.jackson.databind/core` 사용 지점을 `tools.jackson.*` 계열로 정리
  - JSON 직렬화/역직렬화 예외 타입을 `JacksonException` 기준으로 맞춤

## 2. 원본 대비 호환성 결과

- 라우트 호환성
  - 원본 FastAPI(`../server/app/api/v1`)와 Spring 컨트롤러를 자동 대조
  - 결과: `누락 0`, `추가 0` (path+method 기준)

- 동작/계약 호환성
  - 요청/응답은 명시 DTO 기반으로 구현 (임시 `Map` 응답 우회 없음)
  - 상태 전이/권한/검증/연결 동기화/멘션/알림/타임라인 로직 반영
  - Swagger `tag/summary/description` 전 엔드포인트 작성 완료

## 3. 검증 결과

- `./gradlew test -q` 통과
- ArchUnit(`LayerArchitectureRulesTest`) 통과

## 4. 남은 논의점

- `activation/query`의 LLM 오케스트레이션 고도화는 별도 인프라 연동 범위(현재 규칙 기반 질의 로직)

## 5. 미완료 항목 수

- `0`
