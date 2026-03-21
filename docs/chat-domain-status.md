# Chat 도메인 정리

## 남은 의사결정

- 운영 기본 모델을 무엇으로 고정할지 결정해야 합니다.
- quota 초과, LLM 장애, timeout 발생 시 사용자에게 어떤 문구와 재시도 UX를 보여줄지 정해야 합니다.
- prompt, tool arguments, tool result를 로그/감사에 어느 수준까지 남길지 정해야 합니다.
- 채팅 이력, tool call 감사 이력, action draft를 얼마나 오래 보관할지 정해야 합니다.
- 멀티 인스턴스로 확장할 시점에 SSE backplane과 replay 정책을 어떻게 가져갈지 정해야 합니다.

## 현재 구현된 사항

- Spring AI `ChatClient`와 tool calling 기반으로 챗 실행이 동작합니다.
- `part_lookup`, `part_issue_lookup`, `issue_create_draft` 도구가 연결되어 있습니다.
- 쓰기 동작은 직접 실행하지 않고 `draft -> 사용자 확인 -> 실제 실행` 흐름으로 분리되어 있습니다.
- assistant 응답은 일반 텍스트와 UI artifact를 함께 담는 구조로 조립됩니다.
- tool 실행 이력은 별도 audit 모델로 저장되고, tool 단위 OTel 메트릭도 기록됩니다.
- 채팅 문맥은 DB에 저장된 이전 메시지를 다시 읽어 최근 대화는 원문으로, 오래된 대화는 요약으로 주입합니다.
- SSE는 `last_event_sequence`와 `Last-Event-ID`를 이용한 replay를 지원합니다.
- LLM kill switch, timeout, quota 체크, AI usage 적재가 연결되어 있습니다.
- 프롬프트 인젝션에 대해 최소 수준의 시스템 프롬프트 가드가 들어가 있습니다.
- 단일 인스턴스 기준으로는 구조적으로 운영 직전 수준까지 정리되어 있습니다.

## 아직 구현하지 않은 범위

- 실제 provider를 붙인 프론트 E2E smoke test
- 멀티 인스턴스용 Redis backplane
- 장기 대화 summary를 별도 영속화하는 compaction 정책
- 운영 대시보드용 알람/지표 패널 정리
