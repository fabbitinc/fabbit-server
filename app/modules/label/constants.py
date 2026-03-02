"""라벨 도메인 상수."""

# 테넌트 프로비저닝 시 자동 생성되는 기본 라벨
DEFAULT_LABELS = [
    # 우선순위
    {"name": "우선순위:높음", "description": "즉시 처리 필요", "color": "#b60205"},
    {"name": "우선순위:중간", "description": "일반 처리", "color": "#fbca04"},
    {"name": "우선순위:낮음", "description": "여유 시 처리", "color": "#0e8a16"},
    # 유형
    {"name": "설계변경", "description": "설계 도면 또는 사양 변경", "color": "#0075ca"},
    {"name": "품질", "description": "품질 불량 및 결함 보고", "color": "#d73a4a"},
    {"name": "개선", "description": "기존 부품·공정 개선", "color": "#a2eeef"},
    {"name": "원가절감", "description": "원가 절감 활동", "color": "#c5def5"},
    {"name": "공급사", "description": "공급사 관련 문제", "color": "#f9d0c4"},
    {"name": "시험검증", "description": "시험·검증 요청", "color": "#bfd4f2"},
]
