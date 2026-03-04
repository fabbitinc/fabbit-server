"""unit 테스트 공용 설정."""

import pytest


@pytest.hookimpl(tryfirst=True)
def pytest_collection_modifyitems(items):
    """unit 디렉터리 테스트에 공통 marker를 부여한다."""
    for item in items:
        item.add_marker(pytest.mark.unit)
