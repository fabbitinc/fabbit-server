# Entity 변경 필요 목록

- 검토 범위: `src/main/java/com/fabbitinc/server/domain` 하위 `@Entity` 41개
- 기준: `entity-conventions` 스킬의 DDD / Rich Model / ID 중심 참조 / `static factory` / `DomainException`
- 현재는 "엔티티 단독 수정"으로 끝낼 수 없는 항목만 남김

## 남은 구조 이슈

- `src/main/java/com/fabbitinc/server/domain/auth/model/RefreshToken.java`
  만료 검증과 회전 행위는 들어갔지만, 명시적 폐기 상태를 도메인에 올리려면 `revoked_at` 같은 컬럼과 마이그레이션이 먼저 필요합니다.

## 이번 라운드 반영 완료

- 공통: `AbstractIdEntity` ID 기반 동등성, `AggregateRoot` marker, private 생성자 + static factory 기준 적용
- issue / change-request: 루트 메서드로 relation entity 생성 이동, 외부 aggregate ID 중심 정리
- part / team / project / organization: 루트 메서드, 컬렉션 copy 반환, 핵심 상태/수량/역할/쿼터 메서드 보강
- part query 연계: `Part`, `PartDefaultOwner`, `PartRevision`의 보조 relation을 `_...Relation + getter 비노출`로 정리
- subscription / notification / auth: 상태 전이, 읽기 전용 relation, refresh token 라이프사이클 보강
- mapping / file / drawing / label / supplier / user: setter성 메서드 제거, 검증/정규화/의도 메서드 보강
- aiusage / activity / synthesis: public 생성자 제거, 음수/상태 전이 검증, 배치/잡 수명주기 보강

## 특이사항

- `src/main/java/com/fabbitinc/server/domain/auth/model/EmailVerification.java`
  이번 기준에서는 추가 우선순위가 낮습니다.
- query 정리 라운드에서 추가로 남은 entity backlog는 생기지 않았습니다.
