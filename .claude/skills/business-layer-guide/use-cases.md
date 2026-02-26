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

## 규칙

- 비즈니스 로직을 직접 갖지 않음 — service에 위임
- 크로스 도메인 시 여러 service 조합 가능
- 검증, 상태 전이 등은 service/도메인 모델에 위임
- 도메인 모델 메서드 직접 호출 금지 — service 메서드를 통해 간접 호출
- infrastructure(s3_client 등) 직접 import 금지 — service가 infrastructure를 감싸서 제공
- 응답 매핑(schema 변환, URL 생성 등)은 해당 도메인 service에 위임

```
✓ files = file_service.validate(db, file_ids)
  part_service.attach_files(db, part_id, files)
  return file_service.to_file_items(files)

✗ part = part_service.get_or_raise(db, part_id)
  part.attach_files(files)                        ← 도메인 메서드 직접 호출
  return [PartFileItem(..., url=s3.get_url(...))]  ← infrastructure + 응답 매핑 직접 수행
```
