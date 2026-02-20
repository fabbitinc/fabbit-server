"""제조업 Base Ontology — 단일 진실 공급원(SSoT).

시스템 전체에서 참조하는 지식 그래프의 스키마 정의서입니다.
모든 노드 라벨, 관계 타입, 속성 정의를 frozen dataclass로 관리합니다.

사용처:
  - LLM 프롬프트 생성 (매핑, Cypher 변환, 자연어 질의)
  - Excel 컬럼 → 온톨로지 속성 매핑 검증
  - 배치 인제스션 시 MERGE 키 결정 및 데이터 타입 캐스팅
  - DB 인덱스 자동 생성 (is_indexed=True인 속성 기반)

확장 속성 규칙:
  이 파일에 정의되지 않은 컬럼이 인제스션될 경우,
  자동으로 `_ext_` 접두사가 붙어 노드 속성으로 저장됩니다.
  예: Excel의 "무게" 컬럼 → `_ext_weight` 속성으로 저장
  확장 속성은 LLM 질의 시 힌트로 제공되어 쿼리 생성에 활용됩니다.
"""

from dataclasses import dataclass

# ──────────────────────────────────────────────
# 구조체 정의
# ──────────────────────────────────────────────


@dataclass(frozen=True)
class PropertyDef:
    """노드 또는 관계의 개별 속성 정의.

    Attributes:
        name: 속성의 영문 식별자. Cypher 쿼리에서 그대로 사용됨 (예: part_number).
        description: LLM이 Excel 컬럼을 매핑할 때 참고하는 상세 설명.
                     한글로 작성하며, 데이터 예시와 용도를 포함할수록 매핑 정확도가 높아짐.
        data_type: 속성의 데이터 타입. Cypher 값 포맷팅 및 Excel 파싱 시 사용.
                   "string" | "integer" | "float" | "boolean"
        required: True이면 인제스션 시 이 속성이 없는 행은 경고를 발생시킴.
        is_indexed: True이면 AGE 그래프에 인덱스를 생성하여 조회 성능을 최적화.
                    MERGE KEY인 속성은 자동으로 인덱스 대상.
        is_merge_key: True이면 노드의 고유 식별자로 사용.
                      MERGE 시 이 속성으로 기존 노드와 매칭하여 중복 방지.
                      하나의 노드 라벨에 여러 MERGE KEY가 있으면 복합 키로 동작.
    """

    name: str
    description: str
    data_type: str = "string"
    required: bool = False
    is_indexed: bool = False
    is_merge_key: bool = False


@dataclass(frozen=True)
class NodeLabel:
    """그래프 DB의 노드 라벨(엔티티 타입) 정의.

    Attributes:
        label: Cypher에서 사용하는 노드 라벨명 (PascalCase, 예: Part, Supplier).
        description: 이 엔티티가 제조 도메인에서 의미하는 바에 대한 상세 설명.
                     LLM이 자연어 질문을 해석할 때 어떤 노드를 조회해야 하는지 판단하는 근거.
        properties: 이 노드가 가질 수 있는 속성 목록. 순서는 중요도순 권장.
    """

    label: str
    description: str
    properties: tuple[PropertyDef, ...] = ()

    @property
    def merge_keys(self) -> list[str]:
        """MERGE 키로 지정된 속성명 목록"""
        return [p.name for p in self.properties if p.is_merge_key]

    @property
    def required_properties(self) -> list[str]:
        """필수로 지정된 속성명 목록"""
        return [p.name for p in self.properties if p.required]

    @property
    def indexed_properties(self) -> list[str]:
        """인덱스 대상 속성명 목록 (is_merge_key인 속성 포함)"""
        return [p.name for p in self.properties if p.is_indexed or p.is_merge_key]


@dataclass(frozen=True)
class RelationshipType:
    """그래프 DB의 관계 타입 정의.

    Attributes:
        rel_type: Cypher에서 사용하는 관계 타입명 (UPPER_SNAKE_CASE, 예: CONSISTS_OF).
        description: 이 관계가 두 엔티티 사이에서 의미하는 바에 대한 상세 설명.
        from_label: 관계의 출발 노드 라벨.
        to_label: 관계의 도착 노드 라벨.
        properties: 관계 자체에 부여되는 속성 (예: BOM의 수량, 순서).
    """

    rel_type: str
    description: str
    from_label: str
    to_label: str
    properties: tuple[PropertyDef, ...] = ()


@dataclass(frozen=True)
class BaseOntology:
    """전체 온톨로지 스키마 컨테이너.

    제조 도메인의 모든 엔티티와 관계를 하나의 객체로 관리합니다.
    시스템 전체에서 MANUFACTURING_ONTOLOGY 싱글턴을 통해 접근합니다.
    """

    name: str
    description: str
    node_labels: tuple[NodeLabel, ...] = ()
    relationship_types: tuple[RelationshipType, ...] = ()

    # ── 조회 헬퍼 ──

    def get_node_label(self, label: str) -> NodeLabel | None:
        """라벨명으로 NodeLabel 조회"""
        for nl in self.node_labels:
            if nl.label == label:
                return nl
        return None

    def get_valid_labels(self) -> list[str]:
        """정의된 모든 노드 라벨명 목록"""
        return [nl.label for nl in self.node_labels]

    def get_valid_rel_types(self) -> list[str]:
        """정의된 모든 관계 타입명 목록"""
        return [rt.rel_type for rt in self.relationship_types]

    def get_relationship_type(self, rel_type: str) -> RelationshipType | None:
        """관계 타입명으로 RelationshipType 조회"""
        for rt in self.relationship_types:
            if rt.rel_type == rel_type:
                return rt
        return None

    def get_all_indexed_properties(self) -> list[tuple[str, str]]:
        """인덱스가 필요한 (라벨, 속성명) 쌍 목록. DB 인덱스 자동 생성에 사용."""
        result = []
        for nl in self.node_labels:
            for prop_name in nl.indexed_properties:
                result.append((nl.label, prop_name))
        return result

    # ── LLM 프롬프트 생성 ──

    def to_llm_prompt(self) -> str:
        """LLM이 이해하기 쉬운 종합 온톨로지 가이드 텍스트 생성.

        매핑, Cypher 변환, 자연어 질의 등 모든 LLM 호출에서 공통으로 사용합니다.
        to_prompt_text(), to_mapping_prompt_text() 대신 이 메서드를 사용하세요.

        Note:
            다국어 처리는 이 메서드가 아니라 API 응답 레이어에서 담당합니다.
            이 프롬프트의 출력은 JSON/Cypher 등 구조화된 데이터이므로
            온톨로지 설명의 언어가 LLM 출력에 영향을 주지 않습니다.
        """
        lines = [
            f"# {self.name}",
            f"{self.description}",
            "",
            "---",
            "",
        ]

        # 노드 정의
        lines.append("## 노드 (Entities)")
        lines.append("")
        for nl in self.node_labels:
            lines.append(f"### {nl.label}")
            lines.append(f"설명: {nl.description}")
            lines.append(f"MERGE 키: {', '.join(nl.merge_keys)}")
            lines.append("속성:")
            for p in nl.properties:
                flags = []
                if p.is_merge_key:
                    flags.append("MERGE KEY")
                if p.is_indexed:
                    flags.append("INDEXED")
                if p.required:
                    flags.append("필수")
                flag_str = f"  **[{', '.join(flags)}]**" if flags else ""
                lines.append(
                    f"  - `{p.name}` ({p.data_type}): {p.description}{flag_str}"
                )
            lines.append("")

        # 관계 정의
        lines.append("## 관계 (Relationships)")
        lines.append("")
        for rt in self.relationship_types:
            lines.append(f"### {rt.from_label} -[{rt.rel_type}]-> {rt.to_label}")
            lines.append(f"설명: {rt.description}")
            if rt.properties:
                lines.append("속성:")
                for p in rt.properties:
                    lines.append(f"  - `{p.name}` ({p.data_type}): {p.description}")
            lines.append("")

        # 확장 속성 안내
        lines.append("## 확장 속성 (_ext_)")
        lines.append(
            "온톨로지에 정의되지 않은 데이터는 `_ext_` 접두사가 붙어 해당 노드의 속성으로 저장됩니다."
        )
        lines.append("예: Excel '무게' 컬럼 → Part 노드의 `_ext_weight` 속성")
        lines.append("확장 속성도 WHERE 절에서 일반 속성처럼 필터링 가능합니다.")

        return "\n".join(lines)

    def to_prompt_text(self) -> str:
        """LLM Cypher 쿼리 생성용 온톨로지 설명 텍스트 (하위호환)"""
        lines = [f"# {self.name}", f"{self.description}", ""]

        lines.append("## 노드 라벨")
        for nl in self.node_labels:
            lines.append(f"- **{nl.label}**: {nl.description}")
            for p in nl.properties:
                marker = " [MERGE KEY]" if p.is_merge_key else ""
                req = " (필수)" if p.required else ""
                lines.append(
                    f"  - `{p.name}` ({p.data_type}): {p.description}{req}{marker}"
                )
        lines.append("")

        lines.append("## 관계 타입")
        for rt in self.relationship_types:
            lines.append(
                f"- **{rt.from_label} -[{rt.rel_type}]-> {rt.to_label}**: {rt.description}"
            )
            for p in rt.properties:
                lines.append(f"  - `{p.name}` ({p.data_type}): {p.description}")
        return "\n".join(lines)

    def to_mapping_prompt_text(self) -> str:
        """Excel 컬럼 매핑용 상세 텍스트 (하위호환)"""
        lines = [f"# {self.name} - 매핑 가이드", ""]

        for nl in self.node_labels:
            lines.append(f"## {nl.label} ({nl.description})")
            lines.append("속성:")
            for p in nl.properties:
                flags = []
                if p.is_merge_key:
                    flags.append("MERGE KEY - 반드시 매핑 필요")
                if p.required:
                    flags.append("필수")
                flag_str = f" [{', '.join(flags)}]" if flags else ""
                lines.append(f"  - {p.name} ({p.data_type}): {p.description}{flag_str}")
            lines.append("")

        lines.append("## 관계 타입")
        lines.append("")
        for rt in self.relationship_types:
            # 대상 노드의 merge key 조회
            target_node = self.get_node_label(rt.to_label)
            merge_keys = target_node.merge_keys if target_node else []

            lines.append(f"### {rt.from_label} -[{rt.rel_type}]-> {rt.to_label}")
            lines.append(f"설명: {rt.description}")
            lines.append(
                f"대상 노드 MERGE KEY: {', '.join(merge_keys) if merge_keys else '없음'}"
            )
            if rt.properties:
                lines.append("관계 속성 (rel_columns 매핑 대상):")
                for p in rt.properties:
                    lines.append(f"  - {p.name} ({p.data_type}): {p.description}")
            else:
                lines.append("관계 속성: 없음")
            lines.append("")
        return "\n".join(lines)


# ──────────────────────────────────────────────
# 제조업 표준 온톨로지 인스턴스 (SSoT)
# ──────────────────────────────────────────────
# is_merge_key: 식별키

MANUFACTURING_ONTOLOGY = BaseOntology(
    name="Fabbit 제조업 온톨로지",
    description=(
        "부품(Part), 도면(Drawing), 공급사(Supplier), 프로젝트(Project)와 "
        "이들 간의 BOM·도면·공급·소속 관계를 표현하는 제조 도메인 지식 그래프 스키마입니다."
    ),
    node_labels=(
        # ── Part (부품) ──
        NodeLabel(
            label="Part",
            description=(
                "제조 공정에서 관리되는 개별 부품 또는 조립품. "
                "완제품, 반제품, 원자재, 구매품 모두 포함. "
                "BOM(Bill of Materials) 구조에서 상위/하위 관계의 기본 단위."
            ),
            properties=(
                PropertyDef(
                    name="part_number",
                    description=(
                        "품번. 부품의 고유 식별자로 조직 내에서 유일해야 함. "
                        "예: 'ASM-001', 'PRT-1234', 'M-BOLT-10'. "
                        "Excel에서 '품번', '부품번호', 'Part No.', 'P/N' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    required=True,
                    is_indexed=True,
                    is_merge_key=True,
                ),
                PropertyDef(
                    name="name",
                    description=(
                        "부품명. 사람이 읽을 수 있는 부품의 이름이나 명칭. "
                        "예: '메인 프레임', '육각볼트 M10', 'PCB 기판'. "
                        "Excel에서 '품명', '부품명', '명칭', 'Description' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    required=True,
                ),
                PropertyDef(
                    name="revision",
                    description=(
                        "리비전(개정 번호). 부품 설계 변경 이력을 추적하는 버전 식별자. "
                        "예: 'A', 'B', 'Rev.03', '1.2'. "
                        "Excel에서 '리비전', 'Rev', '개정번호' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="material",
                    description=(
                        "재질 또는 소재. 부품의 주요 구성 물질이나 규격. "
                        "예: 'SS400', 'SUS304', 'AL6061-T6', 'ABS', 'POM'. "
                        "Excel에서 '재질', '소재', '재료', 'Material' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="unit",
                    description=(
                        "관리 단위. 부품의 수량을 세는 기본 단위. "
                        "예: 'EA'(개), 'KG'(킬로그램), 'M'(미터), 'SET', 'ROLL'. "
                        "Excel에서 '단위', 'Unit', 'UOM' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="description",
                    description=(
                        "부품에 대한 상세 설명 또는 비고. "
                        "규격, 용도, 특이사항 등 자유 텍스트. "
                        "Excel에서 '설명', '비고', '상세', 'Remark', 'Note' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="category",
                    description=(
                        "부품 분류. 조립품/가공품/구매품 등 관리 유형. "
                        "예: '조립품', '가공품', '구매품', '표준품', '소모품'. "
                        "Excel에서 '분류', '유형', '구분', 'Type', 'Category' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="is_phantom",
                    description=(
                        "가상 조립품(Phantom Assembly) 여부. "
                        "실제 재고로 관리되지 않지만 BOM 구조상 필요한 논리적 조립품을 표시. "
                        "Phantom이면 MRP 전개 시 이 레벨을 건너뛰고 하위 부품을 직접 소요로 계산. "
                        "예: true(가상 조립품), false(실물 부품). "
                        "Excel에서 '팬텀', 'Phantom', '가상조립' 등으로 표기될 수 있음."
                    ),
                    data_type="boolean",
                ),
                PropertyDef(
                    name="lifecycle_state",
                    description=(
                        "부품 수명주기 상태. PLM에서 부품의 성숙도를 추적하는 단계. "
                        "예: 'design'(설계중), 'prototype'(시작품), 'production'(양산), "
                        "'eol'(단종, End of Life), 'obsolete'(폐기). "
                        "프로젝트 상태(Project.status)와 다르며, 부품 자체의 성숙도를 나타냄. "
                        "Excel에서 '수명주기', '상태', 'Lifecycle', 'Phase' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="lead_time_days",
                    description=(
                        "리드타임(일). 부품의 조달 또는 제조에 소요되는 기간(영업일 기준). "
                        "MRP/MPS 산출 시 납기 계산의 핵심 데이터. "
                        "예: 7(1주일), 30(1개월), 90(3개월). "
                        "Excel에서 '리드타임', '납기', 'Lead Time', 'L/T' 등으로 표기될 수 있음."
                    ),
                    data_type="integer",
                ),
            ),
        ),
        # ── Drawing (도면) ──
        NodeLabel(
            label="Drawing",
            description=(
                "부품의 형상, 치수, 공차 등을 정의하는 기술 문서(도면). "
                "CAD 파일, PDF 도면 등을 포함. "
                "하나의 도면이 여러 부품을 정의할 수 있고, 하나의 부품이 여러 도면을 참조할 수 있음."
            ),
            properties=(
                PropertyDef(
                    name="drawing_number",
                    description=(
                        "도면번호. 도면의 고유 식별자로 조직 내에서 유일해야 함. "
                        "예: 'DWG-001', 'A3-FRAME-01', '2024-M-0012'. "
                        "Excel에서 '도면번호', '도번', 'Drawing No.' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    required=True,
                    is_indexed=True,
                    is_merge_key=True,
                ),
                PropertyDef(
                    name="name",
                    description=(
                        "도면명. 도면의 제목이나 명칭. "
                        "예: '메인 프레임 조립도', '브라켓 상세도'. "
                        "Excel에서 '도면명', '도면 이름', 'Title' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="file_path",
                    description=(
                        "도면 파일의 저장 경로 또는 URL. "
                        "S3, R2 등 오브젝트 스토리지 경로 또는 로컬 파일 경로. "
                        "예: 's3://drawings/DWG-001.pdf', '/docs/DWG-001.dwg'."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="version",
                    description=(
                        "도면 버전. 도면의 개정 이력을 추적하는 번호 또는 문자. "
                        "예: '1', '2.1', 'A', 'Rev.C'. "
                        "revision과 유사하지만 도면 자체의 버전을 의미. "
                        "Excel에서 '버전', 'Version', 'Ver' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="status",
                    description=(
                        "도면의 현재 상태. 작성·검토·승인 등 워크플로 단계. "
                        "예: 'draft'(초안), 'in_review'(검토중), 'approved'(승인), "
                        "'released'(배포), 'obsolete'(폐기). "
                        "Excel에서 '상태', '도면상태', 'Status' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
            ),
        ),
        # ── Supplier (공급사) ──
        NodeLabel(
            label="Supplier",
            description=(
                "부품이나 원자재를 공급하는 외부 업체(협력사, 벤더). "
                "하나의 공급사가 여러 부품을 공급할 수 있고, 하나의 부품에 여러 공급사가 있을 수 있음."
            ),
            properties=(
                PropertyDef(
                    name="company_name",
                    description=(
                        "회사명. 공급사의 공식 법인명 또는 상호. "
                        "조직 내 공급사 식별의 기본 키. "
                        "예: '삼성전자', 'MISUMI Korea', '대한볼트공업'. "
                        "Excel에서 '업체명', '공급사', '회사명', 'Supplier', 'Vendor' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    required=True,
                    is_indexed=True,
                    is_merge_key=True,
                ),
                PropertyDef(
                    name="code",
                    description=(
                        "업체 코드. 사내 ERP/MES 시스템에서 사용하는 공급사 고유 코드. "
                        "예: 'SUP-001', 'V-1234'. "
                        "Excel에서 '업체코드', '거래처코드', 'Vendor Code' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    is_indexed=True,
                ),
                PropertyDef(
                    name="country",
                    description=(
                        "국가. 공급사의 소재 국가 또는 주요 생산 거점 국가. "
                        "예: 'KR'(한국), 'JP'(일본), 'CN'(중국), 'US'(미국). "
                        "Excel에서 '국가', '소재지', 'Country' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="contact_info",
                    description=(
                        "연락처 정보. 담당자명, 전화번호, 이메일 등을 포함하는 자유 텍스트. "
                        "예: '김담당 010-1234-5678 kim@supplier.com'. "
                        "Excel에서 '연락처', '담당자', 'Contact' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
            ),
        ),
        # ── Project (프로젝트) ──
        NodeLabel(
            label="Project",
            description=(
                "제품 개발 또는 생산을 위한 프로젝트 단위. "
                "여러 부품(Part)을 포함하며, 일정·담당자·목표 등을 관리. "
                "프로젝트별로 BOM 구조와 부품 목록을 그룹화하는 최상위 컨테이너."
            ),
            properties=(
                PropertyDef(
                    name="name",
                    description=(
                        "프로젝트명. 프로젝트의 고유 식별자이자 이름. "
                        "예: 'EV 모터 하우징 개발', '2024년 신규 브라켓 양산'. "
                        "Excel에서 '프로젝트명', 'Project Name' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    required=True,
                    is_indexed=True,
                    is_merge_key=True,
                ),
                PropertyDef(
                    name="project_code",
                    description=(
                        "프로젝트 코드. 사내 시스템에서 사용하는 프로젝트 고유 코드. "
                        "예: 'PRJ-2024-001', 'EV-MOTOR-V2', 'NPI-BRACKET'. "
                        "Excel에서 '프로젝트코드', '프로젝트번호', 'Project Code' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                    is_indexed=True,
                ),
                PropertyDef(
                    name="manager",
                    description=(
                        "프로젝트 담당자(PM). 프로젝트를 총괄하는 책임자의 이름. "
                        "예: '홍길동', 'John Kim'. "
                        "Excel에서 '담당자', 'PM', 'Manager', '책임자' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="target_date",
                    description=(
                        "목표 완료일. 프로젝트의 계획된 완료 날짜. "
                        "ISO 8601 형식 권장 (YYYY-MM-DD). "
                        "예: '2025-06-30', '2024-12-31'. "
                        "Excel에서 '목표일', '완료예정일', 'Due Date', 'Target Date' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="status",
                    description=(
                        "프로젝트 진행 상태. "
                        "예: 'planning'(기획), 'in_progress'(진행중), "
                        "'completed'(완료), 'on_hold'(보류), 'cancelled'(취소). "
                        "Excel에서 '상태', '진행상태', 'Status' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
            ),
        ),
    ),
    relationship_types=(
        # ── CONSISTS_OF (BOM 관계) ──
        RelationshipType(
            rel_type="CONSISTS_OF",
            description=(
                "BOM(Bill of Materials) 관계. 상위 부품(조립품)이 하위 부품을 포함함을 나타냄. "
                "재귀적 관계로, Part → Part 간 트리 구조를 형성. "
                "예: '메인 프레임 조립품'은 '브라켓' 2개와 '샤프트' 1개로 CONSISTS_OF."
            ),
            from_label="Part",
            to_label="Part",
            properties=(
                PropertyDef(
                    name="quantity",
                    description=(
                        "소요 수량. 상위 부품 1개를 만들기 위해 필요한 하위 부품의 개수. "
                        "예: 2 (브라켓 2개 필요), 4 (볼트 4개 필요). "
                        "Excel에서 '수량', '소요량', 'Qty', 'Quantity' 등으로 표기될 수 있음."
                    ),
                    data_type="integer",
                ),
                PropertyDef(
                    name="sequence",
                    description=(
                        "조립 순서. BOM 내에서 하위 부품의 나열 순서를 나타내는 번호. "
                        "같은 상위 부품 아래에서 정렬 기준으로 사용. "
                        "예: 10, 20, 30 (10 단위로 부여하여 중간 삽입 용이). "
                        "Excel에서 '순서', '시퀀스', 'Seq', 'Item No' 등으로 표기될 수 있음."
                    ),
                    data_type="integer",
                ),
                PropertyDef(
                    name="reference_designator",
                    description=(
                        "참조 지시자. 조립도에서 해당 부품의 위치를 식별하는 기호. "
                        "주로 전자부품 PCB 조립에서 사용 (예: 'R1', 'C3', 'U5'). "
                        "기구부품에서는 잘 사용되지 않음. "
                        "Excel에서 '참조번호', 'Ref Des', 'Reference' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
                PropertyDef(
                    name="find_number",
                    description=(
                        "찾기번호(풍선번호). 조립도 도면 위에 표시된 부품 식별 번호. "
                        "도면의 풍선(balloon) 기호와 BOM 행을 대조하는 데 사용. "
                        "예: '1', '2', '3A', '15'. "
                        "Excel에서 '찾기번호', '풍선번호', 'Find No', 'Item No', 'Balloon' 등으로 표기될 수 있음."
                    ),
                    data_type="string",
                ),
            ),
        ),
        # ── DEFINED_BY (도면 관계) ──
        RelationshipType(
            rel_type="DEFINED_BY",
            description=(
                "부품이 참조하는 도면. 부품의 형상·치수·공차 등이 이 도면에 정의되어 있음. "
                "하나의 부품이 여러 도면을 참조할 수 있음 (조립도 + 상세도 등)."
            ),
            from_label="Part",
            to_label="Drawing",
        ),
        # ── SUPPLIED_BY (공급 관계) ──
        RelationshipType(
            rel_type="SUPPLIED_BY",
            description=(
                "부품의 공급사. 해당 부품을 납품하거나 제조를 담당하는 외부 업체. "
                "하나의 부품에 복수 공급사가 있을 수 있음 (듀얼 소싱 등)."
            ),
            from_label="Part",
            to_label="Supplier",
            properties=(
                PropertyDef(
                    name="unit_cost",
                    description=(
                        "단가. 해당 공급사로부터 이 부품을 공급받을 때의 개당 가격. "
                        "통화 단위는 별도 관리하며 숫자만 저장. "
                        "프로젝트 총 원가 산출의 핵심 데이터. "
                        "예: 1500.0, 25000, 0.5. "
                        "Excel에서 '단가', '가격', 'Unit Price', 'Cost' 등으로 표기될 수 있음."
                    ),
                    data_type="float",
                ),
            ),
        ),
        # ── HAS_ITEM (프로젝트 소속 관계) ──
        RelationshipType(
            rel_type="HAS_ITEM",
            description=(
                "프로젝트에 소속된 부품. 특정 프로젝트에서 사용·개발·관리되는 부품 목록을 나타냄. "
                "프로젝트의 최상위 BOM을 구성하는 품목들과의 연결."
            ),
            from_label="Project",
            to_label="Part",
        ),
    ),
)
