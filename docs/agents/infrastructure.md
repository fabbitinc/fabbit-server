# Infrastructure 작성 규칙

## 역할

- 외부 시스템 추상화 — LLM, S3, AGE, 토큰, 비밀번호 해싱 등
- 교체 시 이 레이어만 수정하면 되도록 격리

## 위치

- `app/infrastructure/{name}.py` — 도메인 모듈 밖에 배치
- 도메인에 종속되지 않는 범용 클라이언트

## 규칙

- service/repository에서 직접 import하여 사용
- 외부 API 에러는 구체적 맥락과 함께 로깅 후 재전파
- 설정값은 `app.core.config.settings`에서 읽기
