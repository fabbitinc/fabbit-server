---
name: testing
description: 통합테스트 진행
allowed-tools:
  - AskUserQuestion
  - Read
  - Glob
  - Grep
  - Edit(scripts/test_full_flow.sh)
  - Bash(curl *)
  - Bash(bash *)
  - Bash(docker *)
disable-model-invocation: true
user-invocable: true
---

## Your task

@scripts/test_full_flow.sh 스크립트를 활용하여 통합테스트를 진행하세요.

### Rules (keep it light)

- 사용자가 데이터베이스 초기화, 서버재실행을 진행합니다.
- 당신은 스크립트만 실행하며, 데이터베이서, 서버에 문제가 있는경우 사용자에게 요청합니다.
- 코드변경으로 인해 스크립트가 동작하지 않을 수 있습니다. 명확한 근거가 있다면 테스트를 수정해서 진행합니다. 그렇지 않다면 사용자에게 요청합니다.
- scripts 진행후 데이터베이스에 직접 접근하여 데이터도 확인합니다. @docker-compose.yml 에서 접속정보를 확인합니다.
