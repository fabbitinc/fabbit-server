---
name: repository-conventions
description: Java Spring Repository 레이어 규칙을 적용한다. 저장소 네이밍, 엔티티 중심 반환, 메서드 시그니처, 영속성 책임 경계 정리가 필요할 때 사용한다.
---

# Repository Conventions

## 목표

- Repository를 영속성 접근 계층으로 유지하라.
- 1단계 기준에서는 엔티티 저장소(`*Repository`)를 기본으로 사용하라.
- 이 스킬은 Repository 규칙만 다루고 다른 레이어 세부 규칙은 다루지 마라.

## 네이밍 규칙

- 엔티티 저장소 인터페이스: `*Repository`
- 메서드명: `save`, `findBy`, `existsBy`, `countBy`, `deleteBy`

## 레이어 규칙

- `*Repository`는 엔티티 중심 계약을 사용하라.
- 단건 조회는 `Optional<Entity>`, 다건 조회는 `List<Entity>`를 기본으로 하라.
- 저장소는 비즈니스 정책을 구현하지 마라.
- 1단계에서는 Query 클래스가 Querydsl/JPA 조회를 직접 구성하고, Repository는 엔티티 저장/조회 책임만 가진다.

## 도메인 의존 규칙

- Repository는 자기 도메인 엔티티 영속성만 다뤄라.
- Repository에서 다른 도메인의 `Repository/Service/UseCase/Query`를 직접 참조하지 마라.

## 적용 절차

1. 엔티티 저장소 인터페이스를 `*Repository`로 정의하라.
2. 저장/조회 메서드를 엔티티 중심 시그니처로 작성하라.
3. 단건/다건 반환 타입을 `Optional/List` 기준으로 맞추라.
4. Query 로직이 Repository로 과도하게 유입되지 않았는지 점검하라.
5. 저장소 코드에 정책 로직이 섞이지 않았는지 점검하라.

## 빠른 체크리스트

- 인터페이스명이 `*Repository` 규칙을 따르는가?
- 엔티티 저장소가 엔티티 타입 중심으로 반환하는가?
- 단건 조회가 `Optional`을 사용하고 있는가?
- 1단계 기준에서 Querydsl 조회 로직이 Query 클래스에 유지되는가?
- 저장소에 비즈니스 정책 로직이 들어가 있지 않은가?
- Repository가 다른 도메인 구현을 직접 참조하지 않는가?
