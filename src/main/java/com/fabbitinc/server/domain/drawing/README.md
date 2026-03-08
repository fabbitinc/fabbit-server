# Fabbit 도면 뷰어/변환 로드맵

## 단계별 비용 요약

| 단계           | 변환 엔진                | 비용                                               | 아키텍처                   |
| -------------- | ------------------------ | -------------------------------------------------- | -------------------------- |
| **MVP**        | 무료 오픈소스 + QCAD CLI | €478 (1회, 서버당)                                 | 모놀리스 (Spring Boot 1대) |
| **Growth**     | ODA Sustaining           | $7,500 첫해 / $4,500 연간 (회사 단위, 서버 무제한) | API + 변환 워커 분리       |
| **Enterprise** | ODA + CAD Exchanger SDK  | +$15K~50K/년 (견적 기반, 매출 연동 티어)           | 동일                       |

---

## 2D 도면 포맷 (많이 쓰는 순)

| 포맷    | 설명                                                | MVP (무료+QCAD)       | Growth (ODA)                             | Enterprise | 변환 타겟           |
| ------- | --------------------------------------------------- | --------------------- | ---------------------------------------- | ---------- | ------------------- |
| **DWG** | AutoCAD 네이티브. 한국 제조업 2D 도면의 사실상 표준 | ✅ QCAD `dwg2pdf`     | ✅ ODA SDK (심층 파싱, 레이어/블록 분석) | ✅         | → PDF               |
| **DXF** | AutoCAD 교환 포맷. DWG 못 열 때 대안                | ✅ QCAD `dwg2pdf`     | ✅ ODA SDK                               | ✅         | → PDF               |
| **PDF** | 최종 배포용. 변환 불필요                            | ✅ PDF.js로 바로 뷰잉 | ✅                                       | ✅         | 변환 타겟 (그 자체) |
| **DWF** | AutoCAD 경량 배포 포맷                              | ❌                    | ✅ ODA 기본 포함                         | ✅         | → PDF               |
| **DGN** | MicroStation (Bentley). 일부 대기업/건설            | ❌                    | ✅ ODA 기본 포함                         | ✅         | → PDF               |

> **2D 변환 전략**: 모든 2D 포맷 → PDF로 변환 후 캐싱. 프론트는 PDF.js 하나로 통일.

---

## 3D 모델 포맷 (많이 쓰는 순)

| 포맷                             | 설명                           | MVP (무료)            | Growth (ODA)     | Enterprise                     | 변환 타겟           |
| -------------------------------- | ------------------------------ | --------------------- | ---------------- | ------------------------------ | ------------------- |
| **STEP/STP**                     | 3D 교환 표준. 제조업 필수      | ✅ occt-import-js     | ✅ ODA 기본 포함 | ✅                             | → glTF(GLB)         |
| **IGES/IGS**                     | 레거시 3D 교환. 아직 현역      | ✅ occt-import-js     | ✅ ODA 기본 포함 | ✅                             | → glTF(GLB)         |
| **STL**                          | 3D 프린팅, 메시 전용           | ✅ Three.js 직접 로드 | ✅ ODA 기본 포함 | ✅                             | 변환 불필요         |
| **OBJ**                          | 범용 메시                      | ✅ Three.js 직접 로드 | ✅ ODA 기본 포함 | ✅                             | 변환 불필요         |
| **glTF/GLB**                     | 웹 3D 표준. 변환 타겟 포맷     | ✅ Three.js 직접 로드 | ✅               | ✅                             | 변환 타겟 (그 자체) |
| **JT**                           | Siemens 경량 3D. 자동차/중공업 | ❌                    | ✅ ODA 기본 포함 | ✅                             | → glTF(GLB)         |
| **3MF**                          | 3D 프린팅 차세대 포맷          | ❌                    | ❌               | ✅ CAD Exchanger               | → glTF(GLB)         |
| **SolidWorks** (.sldprt/.sldasm) | 기계설계 점유율 1위            | ❌                    | ❌               | ✅ CAD Exchanger 또는 ODA MCAD | → glTF(GLB)         |
| **CATIA V5** (.catpart)          | 항공/자동차 대기업             | ❌                    | ❌               | ✅ CAD Exchanger               | → glTF(GLB)         |
| **NX/UG** (.prt)                 | Siemens 네이티브               | ❌                    | ❌               | ✅ CAD Exchanger               | → glTF(GLB)         |
| **Creo/Pro-E** (.prt/.asm)       | PTC 네이티브                   | ❌                    | ❌               | ✅ CAD Exchanger               | → glTF(GLB)         |
| **Inventor** (.ipt/.iam)         | Autodesk 네이티브              | ❌                    | ❌               | ✅ CAD Exchanger 또는 ODA MCAD | → glTF(GLB)         |
| **Parasolid** (.x_t/.x_b)        | 커널 포맷 (SW/NX 등 내부 사용) | ❌                    | ❌               | ✅ CAD Exchanger               | → glTF(GLB)         |

> **3D 변환 전략**: 모든 3D 포맷 → glTF(GLB)로 변환 후 캐싱. 프론트는 Three.js GLTFLoader 하나로 통일.

---

## 단계별 변환 엔진 매핑

| 단계           | 2D 변환              | 3D 변환                             | 프론트 뷰어                   |
| -------------- | -------------------- | ----------------------------------- | ----------------------------- |
| **MVP**        | QCAD CLI (`dwg2pdf`) | occt-import-js (Node.js)            | PDF.js + Three.js             |
| **Growth**     | ODA SDK (QCAD 대체)  | ODA SDK (occt-import-js 대체)       | PDF.js + Three.js (변경 없음) |
| **Enterprise** | ODA SDK (동일)       | ODA + CAD Exchanger (네이티브 추가) | PDF.js + Three.js (변경 없음) |

> Growth 전환 시 ODA 하나가 QCAD + occt-import-js 둘 다 대체. 프론트 코드 수정 없이 백엔드 변환 엔진만 교체.

---

## ODA Sustaining 기본 포함 vs 별도 Extension

### 기본 포함 ($7,500 첫해 / $4,500 연간)

| SDK              | 포맷                                                                          |
| ---------------- | ----------------------------------------------------------------------------- |
| Drawings SDK     | DWG, DXF, DGN, DWF, PDF(export), SVG(export), STL, OBJ, DAE, Three.js(export) |
| STEP SDK         | STEP AP203/AP214/AP242                                                        |
| IGES             | IGES                                                                          |
| JT               | JT (Siemens)                                                                  |
| IFC SDK          | IFC 2x3, 4x0, 4x2, 4x3                                                        |
| Publish SDK      | 3D PDF (PRC)                                                                  |
| Visualize SDK    | 렌더링/뷰어 엔진                                                              |
| Architecture SDK | AutoCAD Architecture 커스텀 객체                                              |
| QIF              | 품질검사 데이터                                                               |

### 별도 유료 Extension (Sustaining 필수)

| Extension          | 가격        | 포맷                                         |
| ------------------ | ----------- | -------------------------------------------- |
| BimRv (Revit)      | +$6,250/년  | .rvt, .rfa                                   |
| BimNv (Navisworks) | +$6,250/년  | .nwd, .nwc, .nwf                             |
| Civil (Civil 3D)   | +$10,000/년 | Civil 3D 커스텀 객체                         |
| Mechanical         | +$12,500/년 | AutoCAD Mechanical 객체                      |
| MCAD               | 별도 문의   | Inventor, CATIA, SolidWorks, Creo, Parasolid |

---

## 아키텍처 전환 타임라인

```
MVP (모놀리스, 스케일 업)
│
│  [Spring Boot]
│  ├── API + 비즈니스 로직
│  ├── ProcessBuilder → QCAD dwg2pdf (2D)
│  ├── ProcessBuilder → Node occt-import-js (3D)
│  └── S3/R2 캐싱
│
│  N100 홈랩 or EC2 1대
│  변환은 @Async 백그라운드
│  고객 ~50개사, 스케일 업으로 대응
│
├── 변환 대기 지속 증가 or DWG 심층 파싱 필요 시
│
Growth (API + 변환 워커 분리)
│
│  [Spring Boot API 서버] ←→ [SQS/Redis] ←→ [ODA 변환 워커]
│  (여러 대 가능)              (큐)            (1대로 충분, 필요시 확장)
│                                              ODA 라이선스 = 회사 단위
│                                              서버 대수 무제한
│
├── 네이티브 CAD 포맷 요구 시
│
Enterprise
│
│  [Spring Boot API] ←→ [큐] ←→ [ODA 워커] + [CAD Exchanger 워커]
│                                              SW/CATIA/NX/Creo 변환
```
