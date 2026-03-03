# use_cases/ 작성 규칙

## 역할

- 오케스트레이션 — router의 쓰기 진입점 + 복잡한 읽기(서비스 조합, LLM 호출 등)

## 위치

- `app/use_cases/{domain}/`

## 트랜잭션

- `@transactional` — 트랜잭션 경계의 소유자 (쓰기)
- `@transactional(read_only=True)` — 복잡한 읽기 오케스트레이션 허용
- UnitOfWork로 commit/rollback 자동 관리

## 호출 대상

- service (단일 도메인이라도 반드시 service 경유)

## 배치 판단 기준

use_case는 **도메인 경계를 넘는 조합**을 위해 존재한다. 같은 도메인 service 함수를 여러 번 호출하고 있다면, 그 로직은 service 내부에 하나의 메서드로 묶여야 한다 — service는 자기 도메인 repo에 전부 접근할 수 있으므로 바깥에서 조율할 구조적 이유가 없다.

```
✓ use_case: project_service.create_project() + label_service.seed_defaults()  ← 다른 도메인 조합
✗ use_case: project_service.create_project() + project_service.add_members()  ← 같은 도메인, service 내부로
```

## 규칙

- 비즈니스 로직을 직접 갖지 않음 — service에 위임
- 크로스 도메인 시 여러 service 조합 가능
- 검증, 상태 전이 등은 service/도메인 모델에 위임
- 도메인 모델 메서드 직접 호출 금지 — service 메서드를 통해 간접 호출
- infrastructure(s3_client 등) 직접 import 금지 — service가 infrastructure를 감싸서 제공
- 응답 매핑(schema 변환, URL 생성 등)은 해당 도메인 mapper에 위임

```
✓ files = file_service.validate(db, file_ids)
  part_service.attach_files(db, part_id, files)
  return to_file_items(files)                     ← mapper 함수 사용

✗ part = part_service.get_or_raise(db, part_id)
  part.attach_files(files)                        ← 도메인 메서드 직접 호출
  return [PartFileItem(..., url=s3.get_url(...))]  ← infrastructure + 응답 매핑 직접 수행
```
