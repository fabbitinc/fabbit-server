# 🤖 Step 4: AI 매핑 및 사용자 검토 (Interactive Bridge)

본 문서는 업로드된 Raw 데이터의 헤더를 시스템의 `Base Ontology`와 지능적으로 연결하고, 사용자가 이를 검증/수정하는 인터랙티브 환경을 정의합니다.

---

## 1. 개요 (Overview)

AI는 초안을 제안하고, 최종 결정은 사용자가 합니다. 이 과정을 통해 시스템은 데이터의 정확성을 확보하고, 사용자는 자신의 데이터가 어떻게 '지식'으로 변하는지 직접 확인하며 신뢰를 쌓습니다.

---

## 2. 화면 구성 (UI/UX Layout: "The Bridge")

가장 핵심이 되는 매핑 화면은 **좌(Raw 데이터) - 우(표준 온톨로지)**를 연결하는 브릿지 형태로 구성합니다.

### 2.1 좌측 영역: 원본 컬럼 (Source Data)

- **컬럼 리스트**: 업로드된 엑셀의 모든 헤더 노출.
- **데이터 샘플**: 컬럼명 아래에 실제 데이터 예시(예: "ASM-001", "M-BOLT")를 작게 표시하여 유저가 어떤 데이터인지 즉시 인지.
- **상태 아이콘**: 매핑 완료(체크), 확인 필요(물음표), 미매핑(공백).

### 2.2 우측 영역: 표준 온톨로지 (Standard Target)

- **라벨 선택기**: 상단 탭이나 드롭다운으로 `Part`, `BOM`, `Supplier`, `Drawing` 등 대상 엔티티 선택.
- **속성 리스트**: 선택된 라벨에 정의된 표준 속성(`part_number`, `material` 등) 나열.
- **확장 영역**: 표준에 없는 데이터가 들어갈 `[새로운 확장 속성(_ext_)으로 생성]` 영역.

### 2.3 중앙 영역: AI 추천 연결선 (Smart Links)

- **신뢰도별 색상**:
  - **Green (High)**: 신뢰도 90% 이상. 자동으로 연결선이 그어짐.
  - **Orange (Medium)**: 신뢰도 60~89%. 점선으로 표시되며 유저의 확인(Click) 필요.
  - **Gray (Unmapped)**: 매핑되지 않은 나머지 컬럼들.

---

## 3. AI 분석 및 매핑 로직 (AI Matching Engine)

### 3.1 신뢰도 점수(Confidence Score) 산출 기준

1. **문구 일치도**: "P/N" ↔ "part_number" (높음)
2. **시맨틱 유사도**: "공급처" ↔ "Supplier" (중간)
3. **데이터 패턴 분석**: 엑셀 데이터가 전부 숫자인데 온톨로지는 `float`인 경우 (높음)
4. **이력 참조**: 이전에 동일한 파일명이나 비슷한 양식에서 매핑했던 기록 (최상)

### 3.2 확장 속성 자동 분류

- AI가 분석하기에 `Base Ontology`에 적합한 자리가 없는 경우, 자동으로 `is_ext=True` 플래그를 세우고 컬럼명을 정규화하여 제안.
- 예: "열처리 규격" → `_ext_heat_treatment`

---

## 4. 사용자 인터랙션 흐름 (User Journey)

1. **자동 매핑 결과 확인**: 화면 진입 시 AI가 1차적으로 연결한 선들을 확인.
2. **미비점 수정**: 오렌지색 점선을 클릭하여 승인하거나, 잘못 연결된 선을 마우스 드래그로 끊고 다른 속성에 연결.
3. **확장 속성 이름 확정**: AI가 제안한 `_ext_` 필드명을 확인하고 필요시 "재질상세" 등으로 수정.
4. **최종 승인 (Confirm)**: 모든 필수 필드(Required)가 매핑되었는지 시스템이 체크 후 '지식화 시작' 버튼 활성화.

---

## 5. API 기술 명세 (Mapping Preview API)

### 5.1 요청 (Request)

- `file_id`: 분석 대상 파일 ID
- `org_id`: 테넌트 식별자

### 5.2 응답 (Response: Preview Object)

```json
{
  "suggestions": [
    {
      "source_column": "Part No.",
      "target_label": "Part",
      "target_property": "part_number",
      "confidence": 98,
      "reason": "Header name match and data format consistent"
    },
    {
      "source_column": "Weight (kg)",
      "target_label": "Part",
      "target_property": "weight",
      "is_ext": true,
      "confidence": 85
    }
  ],
  "unmapped_columns": ["Remark", "CreatedBy"]
}
```

---

## 6. 향후 과제

- [ ] 매핑 인터페이스의 드래그 앤 드롭(DnD) 라이브러리 선정 (예: React Flow 또는 dnd-kit)
- [ ] 매핑 템플릿 저장 기능 (같은 엑셀 양식을 다시 올릴 때 재사용)
- [ ] 복합 키(Composite Key) 매핑 지원 로직
