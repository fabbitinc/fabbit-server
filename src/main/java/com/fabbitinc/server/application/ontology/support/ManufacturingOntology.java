package com.fabbitinc.server.application.ontology.support;

import java.util.List;

public final class ManufacturingOntology {

    public static final OntologyDef ONTOLOGY = new OntologyDef(
            "Fabbit 제조업 온톨로지",
            "부품(Part), 도면(Drawing), 공급사(Supplier), 프로젝트(Project)와 이들 간의 BOM·도면·공급·소속 관계를 표현하는 제조 도메인 지식 그래프 스키마입니다.",
            List.of(
                    node(
                            "Part",
                            "제조 공정에서 관리되는 개별 부품 또는 조립품",
                            "제조 공정에서 관리되는 개별 부품 또는 조립품. 완제품, 반제품, 원자재, 구매품 모두 포함. BOM(Bill of Materials) 구조에서 상위/하위 관계의 기본 단위.",
                            List.of(
                                    property(
                                            "part_number",
                                            "품번",
                                            PropertyDataType.STRING,
                                            true,
                                            true,
                                            true,
                                            "부품의 고유 식별자로 조직 내에서 유일해야 함",
                                            List.of("'ASM-001'", "'PRT-1234'", "'M-BOLT-10'"),
                                            List.of("'품번'", "'부품번호'", "'Part No.'", "'P/N'")
                                    ),
                                    property(
                                            "name",
                                            "부품명",
                                            PropertyDataType.STRING,
                                            true,
                                            false,
                                            false,
                                            "사람이 읽을 수 있는 부품의 이름이나 명칭",
                                            List.of("'메인 프레임'", "'육각볼트 M10'", "'PCB 기판'"),
                                            List.of("'품명'", "'부품명'", "'명칭'", "'Description'")
                                    ),
                                    property(
                                            "revision",
                                            "리비전",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "부품 설계 변경 이력을 추적하는 버전 식별자",
                                            List.of("'A'", "'B'", "'Rev.03'", "'1.2'"),
                                            List.of("'리비전'", "'Rev'", "'개정번호'")
                                    ),
                                    property(
                                            "material",
                                            "재질",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "부품의 주요 구성 물질이나 규격",
                                            List.of("'SS400'", "'SUS304'", "'AL6061-T6'", "'ABS'", "'POM'"),
                                            List.of("'재질'", "'소재'", "'재료'", "'Material'")
                                    ),
                                    property(
                                            "unit",
                                            "단위",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "부품의 수량을 세는 기본 단위",
                                            List.of("'EA'(개)", "'KG'(킬로그램)", "'M'(미터)", "'SET'", "'ROLL'"),
                                            List.of("'단위'", "'Unit'", "'UOM'")
                                    ),
                                    property(
                                            "description",
                                            "설명",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "규격, 용도, 특이사항 등 자유 텍스트 비고",
                                            List.of(),
                                            List.of("'설명'", "'비고'", "'상세'", "'Remark'", "'Note'")
                                    ),
                                    property(
                                            "category",
                                            "분류",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "조립품, 가공품, 구매품 등 관리 유형",
                                            List.of("'조립품'", "'가공품'", "'구매품'", "'표준품'", "'소모품'"),
                                            List.of("'분류'", "'유형'", "'구분'", "'Type'", "'Category'")
                                    ),
                                    property(
                                            "is_phantom",
                                            "팬텀 조립품 여부",
                                            PropertyDataType.BOOLEAN,
                                            false,
                                            false,
                                            false,
                                            "실제 재고로 관리되지 않지만 BOM 구조상 필요한 논리적 조립품을 표시. Phantom이면 MRP 전개 시 이 레벨을 건너뛰고 하위 부품을 직접 소요로 계산",
                                            List.of("true(가상 조립품)", "false(실물 부품)"),
                                            List.of("'팬텀'", "'Phantom'", "'가상조립'")
                                    ),
                                    property(
                                            "lifecycle_state",
                                            "수명주기 상태",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "PLM에서 부품의 성숙도를 추적하는 단계. 프로젝트 상태(Project.status)와 다르며, 부품 자체의 성숙도를 나타냄",
                                            List.of("'DESIGN'(설계중)", "'PROTOTYPE'(시작품)", "'PRODUCTION'(양산)", "'EOL'(단종)", "'OBSOLETE'(폐기)"),
                                            List.of("'수명주기'", "'상태'", "'Lifecycle'", "'Phase'")
                                    ),
                                    property(
                                            "lead_time_days",
                                            "리드타임(일)",
                                            PropertyDataType.INTEGER,
                                            false,
                                            false,
                                            false,
                                            "부품의 조달 또는 제조에 소요되는 기간(영업일 기준). MRP/MPS 산출 시 납기 계산의 핵심 데이터",
                                            List.of("7(1주일)", "30(1개월)", "90(3개월)"),
                                            List.of("'리드타임'", "'납기'", "'Lead Time'", "'L/T'")
                                    )
                            )
                    ),
                    node(
                            "Drawing",
                            "부품의 형상, 치수, 공차를 정의하는 기술 문서",
                            "부품의 형상, 치수, 공차 등을 정의하는 기술 문서(도면). CAD 파일, PDF 도면 등을 포함. 하나의 도면이 여러 부품을 정의할 수 있고, 하나의 부품이 여러 도면을 참조할 수 있음.",
                            List.of(
                                    property(
                                            "drawing_number",
                                            "도면번호",
                                            PropertyDataType.STRING,
                                            true,
                                            true,
                                            true,
                                            "도면의 고유 식별자로 조직 내에서 유일해야 함",
                                            List.of("'DWG-001'", "'A3-FRAME-01'", "'2024-M-0012'"),
                                            List.of("'도면번호'", "'도번'", "'Drawing No.'")
                                    ),
                                    property(
                                            "name",
                                            "도면명",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "도면의 제목이나 명칭",
                                            List.of("'메인 프레임 조립도'", "'브라켓 상세도'"),
                                            List.of("'도면명'", "'도면 이름'", "'Title'")
                                    ),
                                    property(
                                            "file_path",
                                            "파일 경로",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "S3, R2 등 오브젝트 스토리지 경로 또는 로컬 파일 경로",
                                            List.of("'s3://drawings/DWG-001.pdf'", "'/docs/DWG-001.dwg'"),
                                            List.of()
                                    ),
                                    property(
                                            "version",
                                            "버전",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "도면의 개정 이력을 추적하는 번호 또는 문자. revision과 유사하지만 도면 자체의 버전을 의미",
                                            List.of("'1'", "'2.1'", "'A'", "'Rev.C'"),
                                            List.of("'버전'", "'Version'", "'Ver'")
                                    ),
                                    property(
                                            "status",
                                            "상태",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "작성, 검토, 승인 등 워크플로 단계",
                                            List.of("'draft'(초안)", "'in_review'(검토중)", "'approved'(승인)", "'released'(배포)", "'obsolete'(폐기)"),
                                            List.of("'상태'", "'도면상태'", "'Status'")
                                    )
                            )
                    ),
                    node(
                            "Supplier",
                            "부품이나 원자재를 공급하는 외부 업체",
                            "부품이나 원자재를 공급하는 외부 업체(협력사, 벤더). 하나의 공급사가 여러 부품을 공급할 수 있고, 하나의 부품에 여러 공급사가 있을 수 있음.",
                            List.of(
                                    property(
                                            "company_name",
                                            "회사명",
                                            PropertyDataType.STRING,
                                            true,
                                            true,
                                            true,
                                            "공급사의 공식 법인명 또는 상호. 조직 내 공급사 식별의 기본 키",
                                            List.of("'삼성전자'", "'MISUMI Korea'", "'대한볼트공업'"),
                                            List.of("'업체명'", "'공급사'", "'회사명'", "'Supplier'", "'Vendor'")
                                    ),
                                    property(
                                            "code",
                                            "업체 코드",
                                            PropertyDataType.STRING,
                                            false,
                                            true,
                                            false,
                                            "사내 ERP/MES 시스템에서 사용하는 공급사 고유 코드",
                                            List.of("'SUP-001'", "'V-1234'"),
                                            List.of("'업체코드'", "'거래처코드'", "'Vendor Code'")
                                    ),
                                    property(
                                            "country",
                                            "국가",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "공급사의 소재 국가 또는 주요 생산 거점 국가",
                                            List.of("'KR'(한국)", "'JP'(일본)", "'CN'(중국)", "'US'(미국)"),
                                            List.of("'국가'", "'소재지'", "'Country'")
                                    ),
                                    property(
                                            "contact_info",
                                            "연락처 정보",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "담당자명, 전화번호, 이메일 등을 포함하는 자유 텍스트",
                                            List.of("'김담당 010-1234-5678 kim@supplier.com'"),
                                            List.of("'연락처'", "'담당자'", "'Contact'")
                                    )
                            )
                    ),
                    node(
                            "Project",
                            "제품 개발 또는 생산을 위한 프로젝트 단위",
                            "제품 개발 또는 생산을 위한 프로젝트 단위. 여러 부품(Part)을 포함하며, 일정, 담당자, 목표 등을 관리. 프로젝트별로 BOM 구조와 부품 목록을 그룹화하는 최상위 컨테이너.",
                            List.of(
                                    property(
                                            "name",
                                            "프로젝트명",
                                            PropertyDataType.STRING,
                                            true,
                                            true,
                                            true,
                                            "프로젝트의 고유 식별자이자 이름",
                                            List.of("'EV 모터 하우징 개발'", "'2024년 신규 브라켓 양산'"),
                                            List.of("'프로젝트명'", "'Project Name'")
                                    ),
                                    property(
                                            "project_code",
                                            "프로젝트 코드",
                                            PropertyDataType.STRING,
                                            false,
                                            true,
                                            false,
                                            "사내 시스템에서 사용하는 프로젝트 고유 코드",
                                            List.of("'PRJ-2024-001'", "'EV-MOTOR-V2'", "'NPI-BRACKET'"),
                                            List.of("'프로젝트코드'", "'프로젝트번호'", "'Project Code'")
                                    ),
                                    property(
                                            "manager",
                                            "담당자(PM)",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "프로젝트를 총괄하는 책임자의 이름",
                                            List.of("'홍길동'", "'John Kim'"),
                                            List.of("'담당자'", "'PM'", "'Manager'", "'책임자'")
                                    ),
                                    property(
                                            "target_date",
                                            "목표 완료일",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "프로젝트의 계획된 완료 날짜. ISO 8601 형식 권장 (YYYY-MM-DD)",
                                            List.of("'2025-06-30'", "'2024-12-31'"),
                                            List.of("'목표일'", "'완료예정일'", "'Due Date'", "'Target Date'")
                                    ),
                                    property(
                                            "status",
                                            "진행 상태",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "프로젝트 진행 상태",
                                            List.of("'planning'(기획)", "'in_progress'(진행중)", "'completed'(완료)", "'on_hold'(보류)", "'cancelled'(취소)"),
                                            List.of("'상태'", "'진행상태'", "'Status'")
                                    )
                            )
                    )
            ),
            List.of(
                    relationship(
                            RelationshipType.CONSISTS_OF,
                            "상위 부품이 하위 부품을 포함하는 BOM 관계",
                            "BOM(Bill of Materials) 관계. 상위 부품(조립품)이 하위 부품을 포함함을 나타냄. 재귀적 관계로, Part -> Part 간 트리 구조를 형성. 예: '메인 프레임 조립품'은 '브라켓' 2개와 '샤프트' 1개로 CONSISTS_OF.",
                            "Part",
                            "Part",
                            List.of(
                                    property(
                                            "quantity",
                                            "소요 수량",
                                            PropertyDataType.FLOAT,
                                            false,
                                            false,
                                            false,
                                            "상위 부품 1개를 만들기 위해 필요한 하위 부품의 개수",
                                            List.of("2 (브라켓 2개 필요)", "4 (볼트 4개 필요)"),
                                            List.of("'수량'", "'소요량'", "'Qty'", "'Quantity'")
                                    ),
                                    property(
                                            "sequence",
                                            "조립 순서",
                                            PropertyDataType.INTEGER,
                                            false,
                                            false,
                                            false,
                                            "BOM 내에서 하위 부품의 나열 순서를 나타내는 번호. 같은 상위 부품 아래에서 정렬 기준으로 사용",
                                            List.of("10", "20", "30 (10 단위로 부여하여 중간 삽입 용이)"),
                                            List.of("'순서'", "'시퀀스'", "'Seq'", "'Item No'")
                                    ),
                                    property(
                                            "reference_designator",
                                            "참조 지시자",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "조립도에서 해당 부품의 위치를 식별하는 기호. 주로 전자부품 PCB 조립에서 사용",
                                            List.of("'R1'", "'C3'", "'U5'"),
                                            List.of("'참조번호'", "'Ref Des'", "'Reference'")
                                    ),
                                    property(
                                            "find_number",
                                            "찾기번호",
                                            PropertyDataType.STRING,
                                            false,
                                            false,
                                            false,
                                            "조립도 도면 위에 표시된 부품 식별 번호. 도면의 풍선(balloon) 기호와 BOM 행을 대조하는 데 사용",
                                            List.of("'1'", "'2'", "'3A'", "'15'"),
                                            List.of("'찾기번호'", "'풍선번호'", "'Find No'", "'Item No'", "'Balloon'")
                                    )
                            )
                    ),
                    relationship(
                            RelationshipType.DEFINED_BY,
                            "부품이 참조하는 도면 관계",
                            "부품이 참조하는 도면. 부품의 형상, 치수, 공차 등이 이 도면에 정의되어 있음. 하나의 부품이 여러 도면을 참조할 수 있음 (조립도 + 상세도 등).",
                            "Part",
                            "Drawing",
                            List.of()
                    ),
                    relationship(
                            RelationshipType.SUPPLIED_BY,
                            "부품의 공급사 관계",
                            "부품의 공급사. 해당 부품을 납품하거나 제조를 담당하는 외부 업체. 하나의 부품에 복수 공급사가 있을 수 있음 (듀얼 소싱 등).",
                            "Part",
                            "Supplier",
                            List.of(
                                    property(
                                            "unit_cost",
                                            "단가",
                                            PropertyDataType.FLOAT,
                                            false,
                                            false,
                                            false,
                                            "해당 공급사로부터 이 부품을 공급받을 때의 개당 가격. 통화 단위는 별도 관리하며 숫자만 저장. 프로젝트 총 원가 산출의 핵심 데이터",
                                            List.of("1500.0", "25000", "0.5"),
                                            List.of("'단가'", "'가격'", "'Unit Price'", "'Cost'")
                                    )
                            )
                    ),
                    relationship(
                            RelationshipType.HAS_ITEM,
                            "프로젝트에 소속된 부품 관계",
                            "프로젝트에 소속된 부품. 특정 프로젝트에서 사용, 개발, 관리되는 부품 목록을 나타냄. 프로젝트의 최상위 BOM을 구성하는 품목들과의 연결.",
                            "Project",
                            "Part",
                            List.of()
                    )
            )
    );

    private ManufacturingOntology() {
    }

    private static NodeLabelDef node(
            String label,
            String description,
            String semanticDescription,
            List<PropertyDef> properties
    ) {
        return new NodeLabelDef(label, description, semanticDescription, properties);
    }

    private static RelationshipTypeDef relationship(
            RelationshipType relType,
            String description,
            String semanticDescription,
            String fromLabel,
            String toLabel,
            List<PropertyDef> properties
    ) {
        return new RelationshipTypeDef(relType, description, semanticDescription, fromLabel, toLabel, properties);
    }

    private static PropertyDef property(
            String name,
            String description,
            PropertyDataType dataType,
            boolean required,
            boolean isIndexed,
            boolean isMergeKey,
            String semanticDescription,
            List<String> examples,
            List<String> aliases
    ) {
        return new PropertyDef(
                name,
                description,
                dataType,
                required,
                isIndexed,
                isMergeKey,
                semanticDescription,
                examples,
                aliases
        );
    }

    public record OntologyDef(
            String name,
            String description,
            List<NodeLabelDef> nodeLabels,
            List<RelationshipTypeDef> relationshipTypes
    ) {
        public NodeLabelDef getNodeLabel(String label) {
            return nodeLabels.stream()
                    .filter(nodeLabelDef -> nodeLabelDef.label().equals(label))
                    .findFirst()
                    .orElse(null);
        }
    }

    public record NodeLabelDef(
            String label,
            String description,
            String semanticDescription,
            List<PropertyDef> properties
    ) {
        public List<String> mergeKeys() {
            return properties.stream()
                    .filter(PropertyDef::isMergeKey)
                    .map(PropertyDef::name)
                    .toList();
        }
    }

    public record RelationshipTypeDef(
            RelationshipType relType,
            String description,
            String semanticDescription,
            String fromLabel,
            String toLabel,
            List<PropertyDef> properties
    ) {
    }

    public record PropertyDef(
            String name,
            String description,
            PropertyDataType dataType,
            boolean required,
            boolean isIndexed,
            boolean isMergeKey,
            String semanticDescription,
            List<String> examples,
            List<String> aliases
    ) {
    }
}
