---
name: query-conventions
description: Java Spring Query 레이어 규칙을 적용한다. 조회 전용 객체 네이밍, Condition/Result 모델, read-only 처리, 목록/상세/lookup 조회 형태 정리가 필요할 때 사용한다.
---

# Query Conventions

## 목표

- Query를 조회 전용(read-only) 계층으로 유지하라.
- 조회 입력/출력 경계를 `Condition/Result`로 명확히 관리하라.
- 이 스킬은 Query 규칙만 다루고 다른 레이어 세부 규칙은 다루지 마라.

## 네이밍 규칙

- 클래스: `*Query`
- 입력 모델: `*Condition`
- 출력 모델: `*Result`
- 메서드명: `list`, `get`, `search`, `lookup`

## 레이어 규칙

- Query는 상태를 변경하지 마라.
- Query 클래스 레벨에 `@Transactional(readOnly = true)`를 선언하라.
- Query 메서드에는 트랜잭션 어노테이션을 중복 선언하지 마라.
- 페이징/커서/필터 파라미터는 `Condition`에 모아 관리하라.
- 조회 결과는 API 응답과 독립된 `Result` 모델로 유지하라.
- 목록/상세/자동완성 용도를 혼합하지 마라.

## 조회 구현 우선순위

- 단순 PK/unique key 조회, 존재 여부 확인, 개수 조회처럼 엔티티 반환/판별만 필요한 경우 `Repository`를 사용하라.
- 조건 조합, 검색, 정렬, 다중 조인, 집계, 프로젝션, 페이지네이션이 필요한 조회는 `QueryDSL`을 기본으로 사용하라.
- `JPQL`은 QueryDSL보다 더 단순하고 읽기 쉬운 짧은 정적 조회에만 예외적으로 사용하라.
- `Native Query`는 재귀 CTE, 윈도우 함수, DB 전용 함수처럼 QueryDSL/JPQL로 표현이 어렵거나 성능 근거가 있는 경우에만 사용하라.
- 팀 기본 선택은 `QueryDSL`과 `Repository`다. 새 조회는 이 두 가지 안에서 먼저 해결하라.

## 조회 조립 규칙

- Query는 엔티티 relation getter를 따라가 응답을 조립하지 말고, 조인/프로젝션 또는 ID 기반 보조 조회로 필요한 데이터를 모아라.
- 복잡 조회의 결과 조합 책임은 Query가 가진다. Repository에 API 응답 DTO 조합 책임을 넘기지 마라.
- Excel/CSV 같은 조회성 export도 입력은 `Condition`으로 받고, 내부 조회 규칙은 일반 Query와 동일하게 유지하라.

## 도메인 의존 규칙

- Query는 자기 도메인의 `Repository`를 기본 참조하라.
- Query에서 다른 도메인의 `Query/Service/Repository`를 직접 참조하지 마라.
- 다른 도메인 정책/데이터가 필요하면 공개된 `*Api/*Policy`만 참조하라.
- `*Api/*Policy`는 interface/class 중 팀 합의된 한 가지 방식으로 일관되게 사용하라. interface를 강제하지 않는다.

## 적용 절차

1. 조회 시나리오를 목록/상세/자동완성 중 하나로 확정하라.
2. 입력 `*Condition`과 출력 `*Result`를 정의하라.
3. 메서드명을 `list/get/search/lookup` 중 의미에 맞게 선택하라.
4. Query 클래스에 `@Transactional(readOnly = true)`를 선언하라.
5. 결과 모델이 조회 용도에 맞는 최소 필드인지 점검하라.

## 빠른 체크리스트

- 클래스명이 `*Query`로 끝나는가?
- 입력/출력이 `*Condition/*Result` 규칙을 따르는가?
- 상태 변경 코드가 없는가?
- Query 클래스에 read-only 트랜잭션이 선언됐는가?
- 목록/상세/자동완성 반환 모델이 분리됐는가?
- 타 도메인 접근이 `*Api/*Policy` 경계로만 이뤄지는가?
- 단순 조회는 `Repository`, 복잡 조회는 `QueryDSL` 기준을 지켰는가?
- 엔티티 relation getter 대신 조인/프로젝션 또는 ID 기반 보조 조회를 사용했는가?
