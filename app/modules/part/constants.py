"""부품(Part) 도메인 상수."""

from enum import Enum


class BomDirection(str, Enum):
    """BOM 전개 방향."""

    FORWARD = "forward"  # 정전개 (하위 부품 탐색)
    REVERSE = "reverse"  # 역전개 (상위 부품 탐색)
