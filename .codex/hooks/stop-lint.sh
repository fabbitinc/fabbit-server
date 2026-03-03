#!/bin/bash
INPUT=$(cat)

# 무한 루프 방지: 이미 hook 결과로 재실행 중이면 중지 허용
if [ "$(echo "$INPUT" | jq -r '.stop_hook_active')" = "true" ]; then
  exit 0
fi

# lint 실행
LINT_OUTPUT=$(make lint 2>&1)
LINT_EXIT=$?

if [ $LINT_EXIT -ne 0 ]; then
  echo "$LINT_OUTPUT" >&2
  exit 2  # 차단: Claude에게 피드백 전달
fi

exit 0  # 성공: Claude 중지 허용
