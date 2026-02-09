"""온톨로지 API 엔드포인트 (v1).

파이프라인 (매핑/인제스션) + 자연어 질의 + Cypher 변환을 통합한 라우터입니다.
"""

import io
import json

import pandas as pd
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.modules.ontology import repository as repo
from app.modules.ontology import service
from app.modules.ontology.schemas import (
    CypherRequest,
    IngestionStats,
    MappingConfirmRequest,
    MappingResult,
    QueryRequest,
    QueryResponse,
)

router = APIRouter(tags=["ontology"])


# === 파일 파싱 유틸 ===

def _read_file_to_dataframe(content: bytes, filename: str) -> pd.DataFrame:
    """파일 바이트를 DataFrame으로 변환 (매직 바이트 + 인코딩 자동 감지)"""
    name_lower = filename.lower()

    is_xlsx = content[:4] == b"PK\x03\x04"
    is_xls = content[:8] == b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1"

    if is_xlsx or (name_lower.endswith(".xlsx") and not is_xls):
        return pd.read_excel(io.BytesIO(content), engine="openpyxl")
    if is_xls or name_lower.endswith(".xls"):
        return pd.read_excel(io.BytesIO(content))

    for encoding in ("utf-8-sig", "utf-8", "utf-16", "cp949", "euc-kr", "latin-1"):
        for sep in (",", "\t", ";"):
            try:
                df = pd.read_csv(io.BytesIO(content), encoding=encoding, sep=sep)
                if len(df.columns) > 1:
                    return df
            except (UnicodeDecodeError, ValueError, pd.errors.ParserError):
                continue
    for encoding in ("utf-8-sig", "utf-16", "cp949", "latin-1"):
        try:
            return pd.read_csv(io.BytesIO(content), encoding=encoding, sep=None, engine="python")
        except Exception:
            continue
    raise ValueError("파일 인코딩 또는 구분자를 인식할 수 없습니다.")


# === 파이프라인: 매핑 ===

@router.post("/pipeline/mapping/preview")
async def mapping_preview(
    file: UploadFile = File(...),
    org_id: str = Form(...),
):
    """Excel 업로드 → LLM 매핑 미리보기 (DB 저장 안됨)"""
    if not file.filename.lower().endswith((".xlsx", ".xls", ".csv")):
        raise HTTPException(status_code=400, detail="Excel(.xlsx/.xls) 또는 CSV 파일만 지원합니다.")

    content = await file.read()
    try:
        df = _read_file_to_dataframe(content, file.filename)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"파일 읽기 실패: {e}")

    headers = list(df.columns)
    sample_rows = df.head(5).fillna("").to_dict(orient="records")

    try:
        mapping = service.generate_mapping(headers, sample_rows)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"매핑 생성 실패: {e}")

    mapping_dump = mapping.model_dump()

    return {
        "org_id": org_id,
        "file_name": file.filename,
        "total_rows": len(df),
        "headers": headers,
        "sample_rows": sample_rows,
        "mapping": mapping_dump,
        "confirm_body": {
            "org_id": org_id,
            "name": file.filename,
            "original_headers": headers,
            "mapping": mapping_dump,
        },
    }


@router.post("/pipeline/mapping/confirm")
def mapping_confirm(req: MappingConfirmRequest, db: Session = Depends(get_db)):
    """매핑 확정 → DB 저장 (Human-in-the-loop)"""
    mapping_json = req.mapping.model_dump()

    result = repo.save_mapping(db, req.org_id, req.name, req.original_headers, mapping_json)
    if not result:
        raise HTTPException(status_code=500, detail="매핑 저장 실패")

    return {
        "id": str(result.id),
        "org_id": result.org_id,
        "name": result.name,
        "created_at": str(result.created_at),
        "mapping": mapping_json,
    }


@router.get("/pipeline/mappings")
def list_mappings(org_id: str, db: Session = Depends(get_db)):
    """저장된 매핑 목록 조회"""
    rows = repo.list_mappings(db, org_id)
    return [
        {
            "id": str(r.id),
            "org_id": r.org_id,
            "name": r.name,
            "original_headers": r.original_headers,
            "mapping": r.mapping,
            "usage_count": r.usage_count,
            "created_at": str(r.created_at),
        }
        for r in rows
    ]


# === 파이프라인: 인제스션 ===

@router.post("/pipeline/ingest", response_model=IngestionStats)
async def ingest(
    file: UploadFile = File(...),
    org_id: str = Form(...),
    mapping_id: str = Form(...),
    db: Session = Depends(get_db),
):
    """Excel + 확정된 매핑 → 배치 인제스션"""
    row = repo.get_mapping(db, mapping_id, org_id)
    if not row:
        raise HTTPException(status_code=404, detail="매핑을 찾을 수 없습니다.")

    mapping_data = row.mapping
    if isinstance(mapping_data, str):
        mapping_data = json.loads(mapping_data)
    mapping = MappingResult(**mapping_data)

    content = await file.read()
    try:
        df = _read_file_to_dataframe(content, file.filename)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"파일 읽기 실패: {e}")

    try:
        stats = service.ingest_dataframe(db, df, mapping, org_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"인제스션 실패: {e}")

    repo.increment_mapping_usage(db, mapping_id)
    return stats


# === 질의 ===

@router.post("/pipeline/query", response_model=QueryResponse)
def query(req: QueryRequest, db: Session = Depends(get_db)):
    """자연어 질의 (테넌트 격리)"""
    try:
        return service.natural_language_query(db, req.question, req.org_id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/ontology/cypher")
def query_cypher(req: CypherRequest, db: Session = Depends(get_db)):
    """자연어 → Cypher 변환 및 실행 (테넌트 격리 없음)"""
    try:
        cypher = service.text_to_cypher(req.question)
        results = service.execute_cypher_query(db, cypher)
        return {"cypher": cypher, "results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
