"""test2 공용 설정.

`-c test2/pytest.ini`로 실행할 때도 프로젝트 루트(`app/`)를 import 가능하게 만든다.
"""

import sys
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


def pytest_addoption(parser):
    parser.addoption(
        "--use-llm",
        action="store_true",
        default=False,
        help="LLM 호출이 필요한 테스트 활성화",
    )


@pytest.fixture(scope="session")
def use_llm(request):
    return request.config.getoption("--use-llm")
