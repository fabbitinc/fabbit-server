"""모델 클래스 상속 순서(MRO) 검증.

규칙: Mixin → Base 순서로 선언. Base/TenantBase는 __bases__의 마지막이어야 한다.

    # 올바름
    class Part(AggregateRoot, UpdatableMixin, PkMixin, TenantBase): ...

    # 위반 — TenantBase 뒤에 Mixin
    class Part(TenantBase, PkMixin): ...
"""

import pytest
from sqlalchemy.orm import DeclarativeBase

from app.core.database import Base, TenantBase, discover_models

# 모델 자동 등록
discover_models()

# DeclarativeBase 계열 클래스 목록
_BASE_CLASSES = frozenset({Base, TenantBase, DeclarativeBase})


def _all_model_classes() -> list[type]:
    """Base, TenantBase의 모든 직접 서브클래스를 수집."""
    return [*Base.__subclasses__(), *TenantBase.__subclasses__()]


def check_base_class_is_last_in_bases():
    """Base/TenantBase는 __bases__의 마지막에 위치해야 한다."""
    violations = []

    for cls in _all_model_classes():
        bases = cls.__bases__
        for i, b in enumerate(bases):
            if b in _BASE_CLASSES and i != len(bases) - 1:
                order = " → ".join(b.__name__ for b in bases)
                violations.append(f"  {cls.__name__}({order}): {b.__name__}은 마지막이어야 함")

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)
