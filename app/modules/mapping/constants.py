"""매핑 도메인 상수."""

from enum import StrEnum


class MappingScope(StrEnum):
    """매핑 스코프 — 매핑 내용 기반 자동 판별.

    - PART_LIST: Part 속성만 존재 (relation 없음). 파일 업로드만으로 합성 가능.
    - FULL_BOM: relation이 존재하고, 모든 대상 노드의 merge key가 파일 컬럼에 매핑됨.
               파일 업로드만으로 합성 가능.
    - ROOT_BOM: relation이 존재하지만 대상 노드의 merge key가 미할당.
               합성 시 root_part_number 등 추가 컨텍스트 입력 필요.
    """

    PART_LIST = "part_list"
    FULL_BOM = "full_bom"
    ROOT_BOM = "root_bom"
