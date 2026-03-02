- 현재: CR → 변경 반영 (리뷰 없이 병합)
- 1단계: CR → 리뷰어 추가 → 승인 → 병합 ← 여기만 하면 됨
- 2단계: CR → 승인 규칙 (N명 필수, 특정 팀 필수) ← 필요할 때
- 3단계: CR → 직무별 워크플로우 단계 ← 훨씬 나중

```python
class Discipline(str, Enum):
    """제조업 PLM 표준 분야 (Part 담당자/팀/Issue/CR 공통)."""

    ALL = "ALL"  # 전체 (총괄)
    DESIGN = "DESIGN"  # 설계
    QUALITY = "QUALITY"  # 품질
    MANUFACTURING = "MANUFACTURING"  # 생산
    PROCUREMENT = "PROCUREMENT"  # 구매
    TEST = "TEST"  # 시험
```

ㅣ
