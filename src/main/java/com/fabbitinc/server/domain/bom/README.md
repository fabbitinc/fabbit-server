# BOM Domain

## 현재 모델

- 현재 시스템은 `EBOM`만 관리한다.
- `EBOM`은 별도 헤더 Aggregate를 두지 않고 `PartRevision`의 하위 스냅샷으로 본다.
- 따라서 도면, 속성, 첨부파일, 공급사, `EBOM` 중 무엇이 바뀌든 상위 `PartRevision`이 새로 올라간다.

현재 엔티티:

- `EngineeringBomItem`
  - 상위 `PartRevision` 하위의 BOM line
  - 핵심 필드
    - `parentPartRevisionId`
    - `lineNumber`
    - `childPartRevisionId`
    - `quantity`
    - `extendedProperties`

현재 제약:

- `lineNumber`는 BOM line 식별자다.
- 같은 자식 부품이 여러 번 들어갈 수 있으므로 unique는 `parentPartRevisionId + lineNumber` 기준으로 잡는다.
- `quantity`는 향후 `MBOM` 확장과 비정수 수량을 고려해 `BigDecimal`을 사용한다.

## 현재 플로우

- `PartRevision` draft 생성 시 이전 revision의 `EngineeringBomItem`을 복제한다.
- BOM 편집은 draft revision에서만 허용한다.
- release 시 `PartRevision`과 해당 `EBOM` 스냅샷이 함께 확정된다.
- 과거 BOM 조회는 특정 `PartRevision`에 연결된 `EngineeringBomItem` 집합을 읽어 재현한다.

## MBOM 확장 방향

향후 제조용 BOM이 필요해지면 `EBOM`을 분리하지 않고 `MBOM`만 별도 도메인으로 추가한다.

원칙:

- `EBOM`은 계속 `PartRevision`을 따른다.
- `MBOM`은 별도 Aggregate/Revision으로 관리한다.
- `MBOM`은 released 된 `PartRevision` 또는 그 `EBOM`을 기반으로 파생한다.

예상 모델:

- `ManufacturingBom`
  - 제조 BOM 헤더
  - 공장/라인/고객 등 운영 스코프 식별
- `ManufacturingBomRevision`
  - `DRAFT`, `APPROVED`, `RELEASED` 등 제조 BOM 버전 상태
- `ManufacturingBomItem`
  - 제조 BOM line
  - 대체부품, 공정용 속성, effectivity 같은 운영 속성 확장 지점

즉 구조는 아래처럼 간다.

- 현재: `PartRevision -> EngineeringBomItem`
- 확장 후: `PartRevision -> EngineeringBomItem`, `ManufacturingBom -> ManufacturingBomRevision -> ManufacturingBomItem`

## 왜 이렇게 나누는가

- 현재 제품은 설계 BOM 중심이므로 `EBOM = PartRevision 하위`가 가장 단순하고 자연스럽다.
- 반면 `MBOM`은 생산/공장/라인/고객별 변형이 필요할 수 있어 별도 lifecycle이 생길 가능성이 높다.
- 따라서 지금은 `EBOM`을 단순하게 유지하고, 복잡성이 생길 때 `MBOM`만 독립시킨다.
