"""LLM 안전성 평가 자리.

현재는 성능/품질 검증을 우선 이관했으며,
안전성 정책 평가는 실제 운영 정책 확정 후 구체화한다.
"""

import pytest

pytestmark = [pytest.mark.eval, pytest.mark.costly]


def test_safety_policy_placeholder():
    pytest.skip("안전성 평가 시나리오 정의 후 활성화 예정")
