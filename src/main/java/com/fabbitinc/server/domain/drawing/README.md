# Fabbit 도면 뷰어/변환 로드맵

## 단계별 비용 요약

| 단계             | 2D 변환                | 3D 변환                    | 비용                                    | 아키텍처                           |
|----------------|----------------------|--------------------------|---------------------------------------|--------------------------------|
| **MVP**        | ezdxf + PDFBox       | Mayo (MayoConv AppImage) | 무료                                    | 모놀리스 (Spring Boot + Docker 1대) |
| **Growth**     | ODA Sustaining SDK   | ODA Sustaining SDK       | $7,500 첫해 / $4,500 연간 (회사 단위, 서버 무제한) | API + 변환 워커 분리                 |
| **Enterprise** | ODA SDK + MCAD 확장 검토 | ODA + CAD Exchanger SDK  | +$15K~50K/년 (견적 기반, 매출 연동 티어)         | 동일                             |

---

## 2D 도면 포맷 (많이 쓰는 순)

| 포맷                       | 설명                                | MVP (현재 서버)       | Growth (ODA)                 | Enterprise | 변환 타겟        |
|--------------------------|-----------------------------------|-------------------|------------------------------|------------|--------------|
| **DXF**                  | AutoCAD 교환 포맷                     | ✅ ezdxf `draw`    | ✅ ODA SDK                    | ✅          | → PDF        |
| **PDF**                  | 최종 배포용. 변환 불필요                    | ✅ PDF.js로 바로 뷰잉   | ✅                            | ✅          | 변환 타겟 (그 자체) |
| **PNG/JPG/BMP/TIF/WEBP** | 스캔본, 협력사 공유본, 문서 캡처               | ✅ PDFBox로 PDF 래핑  | ✅                            | ✅          | → PDF        |
| **DWF**                  | AutoCAD 경량 배포 포맷                  | ❌                 | ✅ ODA 기본 포함                  | ✅          | → PDF        |
| **DGN**                  | MicroStation (Bentley). 일부 대기업/건설 | ❌                 | ✅ ODA 기본 포함                  | ✅          | → PDF        |
| **DWG**                  | AutoCAD 네이티브. 한국 제조업 2D 사실상 표준    | 현재 서버 직접 파이프라인 제외 | ✅ ODA SDK (심층 파싱, 레이어/블록 분석) | ✅          | → PDF        |

> **2D 변환 전략**: 현재 서버는 DXF/PDF/래스터 이미지 중심으로 처리하고, 결과는 PDF로 통일해 S3에 캐싱한다. 프론트는 PDF.js 하나로 통일한다.

### 2D 변환 도구: ezdxf + PDFBox

- **DXF → PDF**: `ezdxf draw` 사용
- **PDF → 프리뷰**: PDFBox 렌더링 사용
- **이미지 → PDF**: PDFBox로 래핑 후 동일한 2D 프리뷰 경로 사용
- **현재 제한**: DWG 직접 변환은 지원하지 않으며 DXF render source를 전제로 한다
- **향후 확장**: DWG/DWF/DGN 같은 네이티브 2D CAD는 ODA 도입 시 검토

---

## 3D 모델 포맷 (많이 쓰는 순)

### 도면(설계) 포맷 — 사용자가 업로드하는 원본

설계자가 CAD 소프트웨어에서 작업한 결과물. B-Rep(정밀 곡면), 어셈블리 구조, PMI(치수/공차) 등 제조에 필요한 정보를 담고 있다.

| 포맷                               | 분류       | 설명                              | MVP (Mayo)        | Growth (ODA) | Enterprise                  |
|----------------------------------|----------|---------------------------------|-------------------|--------------|-----------------------------|
| **STEP/STP**                     | 교환 표준    | ISO 10303. 제조업 3D 도면 교환의 사실상 표준 | ✅ MayoConv        | ✅ ODA 기본 포함  | ✅                           |
| **IGES/IGS**                     | 교환 표준    | 레거시 교환 포맷. STEP 이전 세대, 아직 현역    | ✅ MayoConv        | ✅ ODA 기본 포함  | ✅                           |
| **JT**                           | 경량 뷰잉    | Siemens 경량 3D. 자동차/중공업에서 뷰잉/공유용 | ❌                 | ✅ ODA 기본 포함  | ✅                           |
| **SolidWorks** (.sldprt/.sldasm) | 네이티브 CAD | 기계설계 점유율 1위. 중소~중견 제조업          | 현재 서버 직접 파이프라인 제외 | ❌            | ✅ CAD Exchanger 또는 ODA MCAD |
| **CATIA V5** (.catpart)          | 네이티브 CAD | 항공/자동차 대기업 (현대, 에어버스 등)         | ❌                 | ❌            | ✅ CAD Exchanger             |
| **NX/UG** (.prt)                 | 네이티브 CAD | Siemens 네이티브. 중공업/반도체           | ❌                 | ❌            | ✅ CAD Exchanger             |
| **Creo/Pro-E** (.prt/.asm)       | 네이티브 CAD | PTC 네이티브. 방산/의료기기               | ❌                 | ❌            | ✅ CAD Exchanger             |
| **Inventor** (.ipt/.iam)         | 네이티브 CAD | Autodesk 네이티브                   | ❌                 | ❌            | ✅ CAD Exchanger 또는 ODA MCAD |
| **Parasolid** (.x_t/.x_b)        | 커널 포맷    | SW/NX 등 내부에서 사용하는 B-Rep 커널      | ❌                 | ❌            | ✅ CAD Exchanger             |

### 메시/경량 포맷 — 도면은 아니지만 제조업에서 사용

설계 정밀 데이터(B-Rep)가 없고 삼각형 메시만 담고 있다. 3D 프린팅, 시각화, 협력사 공유 등에 사용. 정밀 치수/곡면 정보가 소실된 상태.

| 포맷      | 분류       | 설명                         | MVP (Mayo)          | Growth (ODA) | Enterprise      |
|---------|----------|----------------------------|---------------------|--------------|-----------------|
| **STL** | 메시       | 3D 프린팅 표준. 색상/어셈블리 없음      | ✅ MayoConv          | ✅ ODA 기본 포함  | ✅               |
| **OBJ** | 메시       | 범용 메시. 간단한 머티리얼 지원         | ✅ MayoConv          | ✅ ODA 기본 포함  | ✅               |
| **3MF** | 메시       | 3D 프린팅 차세대. 색상/소재 포함       | ✅ MayoConv (Assimp) | ❌            | ✅ CAD Exchanger |
| **FBX** | 메시/애니메이션 | Autodesk 교환. 게임/영상에서 주로 사용 | ✅ MayoConv (Assimp) | ❌            | ✅ CAD Exchanger |

### 내부 캐시 포맷 — 사용자가 직접 다루지 않음

| 포맷      | 용도                                                                                                        |
|---------|-----------------------------------------------------------------------------------------------------------|
| **GLB** | Fabbit 내부 3D 뷰잉용 캐시. 모든 3D 원본을 GLB로 변환 후 S3에 저장. Three.js GLTFLoader로 브라우저에서 렌더링. 사용자는 이 포맷의 존재를 인식하지 않음. |
| **PDF** | Fabbit 내부 2D 뷰잉용 캐시. 모든 2D 원본을 PDF로 변환 후 S3에 저장. PDF.js로 브라우저에서 렌더링. (단, 사용자가 PDF를 직접 업로드하는 경우도 있음)       |

> **변환 전략**: 도면 원본 → S3 보관 (다운로드용) + GLB/PDF 캐시 생성 (뷰잉용). 프론트는 PDF.js + Three.js GLTFLoader 두 개만 사용. 사용자가 다운로드하면 원본(DXF,
> STEP/STP, IGES/IGS 등)이 나옴.

### 왜 GLB인가: glTF vs GLB

glTF(GL Transmission Format)는 Khronos Group(OpenGL, Vulkan, WebGL을 만든 곳)이 제정한 웹 3D 공식 표준으로, **"3D의 JPEG"**라고 불린다.

glTF는 두 가지 형태로 존재한다:

```
glTF (텍스트, 여러 파일):          GLB (바이너리, 단일 파일):
├── model.gltf  (JSON 메타)        model.glb  ← 전부 하나에 패킹
├── model.bin   (메시 데이터)
├── texture1.png
└── texture2.jpg
```

- **glTF (.gltf)**: JSON 메타 파일 + .bin(메시) + 텍스처 이미지가 분리. 파일 하나만으로는 열리지 않는 경우가 많음
- **GLB (.glb)**: JSON + bin + 텍스처를 하나의 바이너리로 패킹. **단일 파일로 완전 자족적(self-contained)**

Fabbit에서는 **반드시 GLB**를 사용한다:

- S3에 파일 하나만 올리면 됨 (presigned URL 하나로 로드)
- Three.js GLTFLoader가 .glb도 .gltf도 둘 다 읽지만, .glb가 추가 파일 참조 없이 바로 로드됨
- CAD 모델은 텍스처가 거의 없고 단색 머티리얼 위주라 GLB 패킹에 손실 없음

채택 현황: Google (Android 3D), Meta, Apple (Quick Look), Shopify (상품 3D), Three.js, Babylon.js, Unity, Unreal 전부 GLB 네이티브
지원.

|        | 2D                                 | 3D                                           |
|--------|------------------------------------|----------------------------------------------|
| 원본     | DXF, PDF, PNG/JPG/BMP/TIF/WEBP ... | STEP, STP, IGES, IGS, BREP/BRP, STL, OBJ ... |
| 변환 타겟  | **PDF** (단일 파일)                    | **GLB** (단일 파일)                              |
| 프론트 뷰어 | PDF.js                             | Three.js GLTFLoader                          |
| 원본 보관  | ✅ 다운로드용                            | ✅ 다운로드용 (B-Rep 정밀 데이터)                       |

### 3D 변환 도구: Mayo (MayoConv)

- **GitHub**: fougue/mayo ⭐ 1.9K
- **비용**: 무료 (BSD-2 라이선스)
- **배포**: `MayoConv-0.9.0-x86_64.AppImage` 단일 파일, 의존성 설치 불필요 (self-contained)
- **CLI**: `./MayoConv --input part.stp --output part.glb`
- **엔진**: C++ 네이티브 OpenCascade + Qt, WASM 대비 2~3배 빠름
- **지원 포맷**: STEP, IGES, BREP, OBJ, STL, glTF/GLB, 3MF, FBX, Collada, VRML 등 15+
- **어셈블리/색상**: OCCT XDE 기반으로 파트 트리 + 머티리얼 보존 (BOM 연동에 필수)
- **Docker**: AppImage 복사 후 `--appimage-extract-and-run` 플래그로 FUSE 없이 실행 가능
- **썸네일**: `--output part.png` 로 이미지 출력 지원

---

## 단계별 변환 엔진 매핑

| 단계             | 2D 변환                     | 3D 변환                                          | 프론트 뷰어                    |
|----------------|---------------------------|------------------------------------------------|---------------------------|
| **MVP**        | ezdxf + PDFBox            | MayoConv AppImage (`--input stp --output glb`) | PDF.js + Three.js         |
| **Growth**     | ODA SDK (ezdxf/PDFBox 대체) | ODA SDK (Mayo 대체)                              | PDF.js + Three.js (변경 없음) |
| **Enterprise** | ODA SDK + MCAD 확장         | ODA + CAD Exchanger (네이티브 추가)                  | PDF.js + Three.js (변경 없음) |

> Growth 전환 시 ODA 하나가 ezdxf/PDFBox + Mayo 역할을 함께 대체한다. 프론트 코드 수정 없이 백엔드 변환 엔진만 교체한다.

---

## ODA Sustaining 기본 포함 vs 별도 Extension

### 기본 포함 ($7,500 첫해 / $4,500 연간)

| SDK              | 포맷                                                                            |
|------------------|-------------------------------------------------------------------------------|
| Drawings SDK     | DWG, DXF, DGN, DWF, PDF(export), SVG(export), STL, OBJ, DAE, Three.js(export) |
| STEP SDK         | STEP AP203/AP214/AP242                                                        |
| IGES             | IGES                                                                          |
| JT               | JT (Siemens)                                                                  |
| IFC SDK          | IFC 2x3, 4x0, 4x2, 4x3                                                        |
| Publish SDK      | 3D PDF (PRC)                                                                  |
| Visualize SDK    | 렌더링/뷰어 엔진                                                                     |
| Architecture SDK | AutoCAD Architecture 커스텀 객체                                                   |
| QIF              | 품질검사 데이터                                                                      |

### 별도 유료 Extension (Sustaining 필수)

| Extension          | 가격         | 포맷                                           |
|--------------------|------------|----------------------------------------------|
| BimRv (Revit)      | +$6,250/년  | .rvt, .rfa                                   |
| BimNv (Navisworks) | +$6,250/년  | .nwd, .nwc, .nwf                             |
| Civil (Civil 3D)   | +$10,000/년 | Civil 3D 커스텀 객체                              |
| Mechanical         | +$12,500/년 | AutoCAD Mechanical 객체                        |
| MCAD               | 별도 문의      | Inventor, CATIA, SolidWorks, Creo, Parasolid |

---

## 아키텍처 전환 타임라인

```
MVP (모놀리스, Docker 1대, 스케일 업)
│
│  [Docker Container]
│  ├── Spring Boot JAR (API + 비즈니스 로직)
│  ├── ProcessBuilder → ezdxf draw (DXF → PDF)
│  ├── ProcessBuilder → MayoConv AppImage (3D → GLB)
│  ├── 프리뷰: PDFBox (PDF 렌더링), MayoConv --output png (3D)
│  └── S3/R2 캐싱 (원본 + PDF/GLB + 썸네일 PNG)
│
│  EC2 1대, 변환은 @Async 백그라운드
│  고객 ~50개사, 스케일 업으로 대응
│
├── 변환 대기 지속 증가 or DWG/DGN 같은 네이티브 2D CAD 지원 필요 시
│
Growth (API + 변환 워커 분리)
│
│  [Spring Boot API 서버] ←→ [SQS/Redis] ←→ [ODA 변환 워커]
│  (여러 대 가능)              (큐)            (1대로 충분, 필요시 확장)
│                                              ODA 라이선스 = 회사 단위
│                                              서버 대수 무제한
│                                              2D+3D 모두 ODA 하나로 처리
│
├── 네이티브 CAD 포맷 요구 시
│
Enterprise
│
│  [Spring Boot API] ←→ [큐] ←→ [ODA 워커] + [CAD Exchanger 워커]
│                                              SW/CATIA/NX/Creo 변환
```

---

## MVP Docker 이미지 구성

```dockerfile
FROM eclipse-temurin:21-jre

# 2D: ezdxf CLI (DXF -> PDF)
COPY ezdxf /opt/ezdxf/ezdxf
ENV EZDXF_BIN_PATH=/opt/ezdxf/ezdxf

# 3D: Mayo CLI (AppImage, 무료, 의존성 없음)
COPY MayoConv-0.9.0-x86_64.AppImage /opt/mayo/mayoconv
RUN chmod +x /opt/mayo/mayoconv

# Spring Boot
COPY app.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Spring Boot에서 호출

```java
// 2D CAD: DXF → PDF
new ProcessBuilder(
    "/opt/ezdxf/ezdxf",
            "draw",
            "--backend","matplotlib",
            "--background","WHITE",
            "-f",
            "-o","output.pdf",
            "input.dxf"
)

// 3D: STEP → GLB
new

ProcessBuilder("/opt/mayo/mayoconv",
                       "--appimage-extract-and-run",
                       "--input","input.stp",
                       "--output","output.glb"
)

// 3D 썸네일: STEP → PNG
new

ProcessBuilder("/opt/mayo/mayoconv",
                       "--appimage-extract-and-run",
                       "--input","input.stp",
                       "--output","thumbnail.png"
)
```

### 변환 플로우

```
파일 업로드
  → S3 원본 저장
  → @Async 변환 Job
      ├── 2D (DXF/PDF/PNG/JPG/BMP/TIF/TIFF/WEBP) → ezdxf/PDFBox → PDF + PDF 렌더 프리뷰
      └── 3D (STEP/STP/IGES/IGS/BREP/BRP/STL/OBJ/3MF/FBX/GLB/GLTF) → MayoConv → GLB + PNG 썸네일
  → S3에 PDF/GLB + 썸네일 PNG 캐싱
  → DB 상태 업데이트 (변환완료)

조회
  → 캐싱된 PDF → PDF.js
  → 캐싱된 GLB → Three.js GLTFLoader
  → 원본 다운로드 → S3 presigned URL
```
