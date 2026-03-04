---
name: logging-guide
description: OTel 환경에서 Java Spring 로그 작성 규칙을 적용한다. logger 선언, 로그 레벨 선택, 비즈니스 로그 작성, 레이어별 로깅 기준이 필요할 때 사용한다.
---

# Logging Guide (OTel-Native)

## 전제

- OTel 추적 연동(trace/span 상관관계)은 이미 구성되어 있다고 가정한다.
- 이 문서는 로깅 "설정"이 아니라 로깅 "작성 규칙"만 다룬다.

## OTel vs 수동 로그 역할

| 항목 | OTel(자동) | 수동 로그 |
|---|---|---|
| HTTP 요청/응답, DB 호출, 외부 API 호출, 수행 시간 | 담당 | 작성 금지 |
| 비즈니스 분기 이유 | 미제공 | 반드시 기록 |
| 스킵/폴백 사유 | 미제공 | 반드시 기록 |
| 실패의 구체 원인 | 제한적 | 반드시 기록 |

## 로그 레벨

- `DEBUG`: 개발/로컬 진단용, 운영 기본 OFF
- `INFO`: 감사/추적 가치가 있는 상태 변경 이벤트만 기록
- `WARN`: 예상 가능한 예외 상황(스킵, 폴백, 재시도 예정)
- `ERROR`: 요청/처리 실패(복구 불가 또는 사용자 영향)

## 작성 규칙

1. 클래스 로거 선언은 `@Slf4j` 어노테이션을 사용하라.
2. 구조화 로그로 작성하라(키-값 형태).
3. 문자열 결합 로깅을 금지하고, 파라미터/키-값 방식으로 작성하라.
4. 로그 1건은 의미 있는 이벤트 1개만 표현하라.
5. 민감정보(비밀번호, 토큰, API 키, 주민번호/전화번호 원문)를 기록하지 마라.
6. 같은 사실을 여러 레이어에서 중복 로깅하지 마라.
7. 예외 로그는 원인 예외를 함께 전달하라.
8. 정상 성공을 전부 `INFO`로 남기지 마라.

## 레이어별 규칙

- Controller:
  - 정상 요청 진입/종료 로그는 남기지 마라.
  - 보안/인가 실패, 비정상 접근만 `WARN`으로 기록하라.

- UseCase:
  - 비즈니스 상태 전환 결과와 분기 이유를 기록하라.
  - 성공 로그는 중요 상태 변경일 때만 `INFO`로 기록하라.
  - 정책 위반/거부는 `WARN`, 실패는 `ERROR`로 기록하라.

- Query:
  - 정상 조회 성공 로그는 기본적으로 생략하라.
  - 스킵/거부/비정상 조건만 기록하라.

- Service:
  - 도메인 분기 근거(왜 이 경로를 택했는지)를 기록하라.

- Repository:
  - 영속성 실패 시점의 최소 맥락만 기록하라(엔티티 종류, 식별자, 연산).

- EventHandler:
  - 이벤트 처리 결과는 중요 이벤트만 `INFO`로 기록하라.
  - 멱등 스킵/재시도 대상은 `WARN`으로 기록하라.

## 예시

```java
log.info("event=project_created project_id={} org_id={} outcome=success", projectId, orgId);

log.warn("event=quota_rejected org_id={} action={} reason={}", orgId, action, reason);

log.error("event=file_attach_failed file_id={} reason={}", fileId, reason, ex);
```
