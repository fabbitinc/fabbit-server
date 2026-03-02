"""부품(Part) 도메인 상수."""

from enum import Enum


class BomDirection(str, Enum):
    """BOM 전개 방향."""

    FORWARD = "forward"  # 정전개 (하위 부품 탐색)
    REVERSE = "reverse"  # 역전개 (상위 부품 탐색)


class Discipline(str, Enum):
    """제조업 PLM 표준 분야 (Part 담당자/팀/Issue/CR 공통)."""

    ALL = "ALL"  # 전체 (총괄)
    DESIGN = "DESIGN"  # 설계
    QUALITY = "QUALITY"  # 품질
    MANUFACTURING = "MANUFACTURING"  # 생산
    PROCUREMENT = "PROCUREMENT"  # 구매
    TEST = "TEST"  # 시험
