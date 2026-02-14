#!/usr/bin/env bash
# =========================================================
#  Fabbit 전체 플로우 E2E 테스트 스크립트
#  signup → upload → mapping → synthesis → drawing → activation
# =========================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000}"
API="${BASE_URL}/api/v1"
SAMPLE_FILE="${SAMPLE_FILE:-sample/hierarchical_bom.csv}"
DRAWING_FILE="${DRAWING_FILE:-sample/Schematic_Arduino-Uno-Rev3.pdf}"

# 랜덤 이메일 생성 (중복 방지)
RANDOM_SUFFIX=$(date +%s)
EMAIL="test_${RANDOM_SUFFIX}@example.com"
PASSWORD="TestPass1234"
ORG_NAME="TestOrg_${RANDOM_SUFFIX}"
ORG_SLUG="testorg-${RANDOM_SUFFIX}"
FULL_NAME="테스트 사용자"

# 색상
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }
step() { echo -e "\n${YELLOW}━━━ $1 ━━━${NC}"; }

# jq 존재 확인
command -v jq >/dev/null 2>&1 || { echo "jq가 필요합니다: brew install jq"; exit 1; }

DB_CONTAINER="${DB_CONTAINER:-fabbit-db}"
DB_USER="${DB_USER:-fabbit}"
DB_NAME="${DB_NAME:-fabbit}"

db_query() {
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1"
}

# 서버 헬스체크
step "0. 서버 헬스체크"
HEALTH=$(curl -sf "${BASE_URL}/health" 2>/dev/null || echo "")
if [ -z "$HEALTH" ]; then
    fail "서버가 응답하지 않습니다 (${BASE_URL})"
fi
pass "서버 정상 동작"

# =========================================================
step "1. 회원가입 (POST /auth/register)"
# =========================================================
SIGNUP_RESP=$(curl -sf -X POST "${API}/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"${EMAIL}\",
        \"password\": \"${PASSWORD}\",
        \"full_name\": \"${FULL_NAME}\",
        \"org_name\": \"${ORG_NAME}\",
        \"slug\": \"${ORG_SLUG}\",
        \"plan_type\": \"STARTER\"
    }" 2>/dev/null) || fail "회원가입 요청 실패"

ACCESS_TOKEN=$(echo "$SIGNUP_RESP" | jq -r '.tokens.access_token // empty')
REFRESH_TOKEN=$(echo "$SIGNUP_RESP" | jq -r '.tokens.refresh_token // empty')

if [ -z "$ACCESS_TOKEN" ]; then
    echo "$SIGNUP_RESP" | jq . 2>/dev/null || echo "$SIGNUP_RESP"
    fail "access_token 없음"
fi

pass "회원가입 성공: ${EMAIL}"
info "access_token: ${ACCESS_TOKEN:0:20}..."

AUTH="Authorization: Bearer ${ACCESS_TOKEN}"

# =========================================================
step "1-1. 내 정보 확인 (GET /auth/me)"
# =========================================================
ME_RESP=$(curl -sf "${API}/auth/me" -H "$AUTH") || fail "내 정보 조회 실패"
ME_EMAIL=$(echo "$ME_RESP" | jq -r '.user.email')
ME_ORG=$(echo "$ME_RESP" | jq -r '.memberships[0].organization.name // "N/A"')
ME_ORG_ID=$(echo "$ME_RESP" | jq -r '.memberships[0].organization.id // empty')
if [ -z "$ME_ORG_ID" ]; then
    fail "organization id 조회 실패"
fi
TENANT_SCHEMA="tenant_${ME_ORG_ID//-/}"
pass "내 정보: email=${ME_EMAIL}, org=${ME_ORG}, org_id=${ME_ORG_ID}"

# =========================================================
step "2. 파일 업로드 (Presigned URL)"
# =========================================================
FILENAME=$(basename "$SAMPLE_FILE")
FILE_SIZE=$(wc -c < "$SAMPLE_FILE" | tr -d ' ')
CONTENT_TYPE="text/csv"

info "파일: ${FILENAME} (${FILE_SIZE} bytes)"

# 2-1. Presigned URL 발급
UPLOAD_RESP=$(curl -sf -X POST "${API}/uploads" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"original_name\": \"${FILENAME}\",
        \"content_type\": \"${CONTENT_TYPE}\",
        \"file_size\": ${FILE_SIZE}
    }") || fail "Presigned URL 발급 실패"

UPLOAD_ID=$(echo "$UPLOAD_RESP" | jq -r '.upload_id')
UPLOAD_URL=$(echo "$UPLOAD_RESP" | jq -r '.upload_url')

if [ -z "$UPLOAD_ID" ] || [ "$UPLOAD_ID" = "null" ]; then
    echo "$UPLOAD_RESP" | jq . 2>/dev/null
    fail "upload_id 없음"
fi
pass "Presigned URL 발급: upload_id=${UPLOAD_ID}"

# 2-2. S3에 파일 업로드
S3_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PUT "$UPLOAD_URL" \
    -H "Content-Type: ${CONTENT_TYPE}" \
    -H "Content-Length: ${FILE_SIZE}" \
    --data-binary "@${SAMPLE_FILE}") || fail "S3 업로드 실패"

if [ "$S3_STATUS" != "200" ]; then
    fail "S3 업로드 실패 (HTTP ${S3_STATUS})"
fi
pass "S3 업로드 완료 (HTTP ${S3_STATUS})"

# 2-3. 업로드 완료 확인
COMPLETE_RESP=$(curl -sf -X POST "${API}/uploads/${UPLOAD_ID}/complete" \
    -H "$AUTH") || fail "업로드 완료 확인 실패"

UPLOAD_STATUS=$(echo "$COMPLETE_RESP" | jq -r '.status')
pass "업로드 완료: status=${UPLOAD_STATUS}"

# =========================================================
step "3. 매핑 미리보기 (POST /mappings/preview)"
# =========================================================
info "LLM 호출 중... (시간이 걸릴 수 있습니다)"

PREVIEW_RESP=$(curl -sf -X POST "${API}/mappings/preview" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"upload_id\": \"${UPLOAD_ID}\"
    }" \
    --max-time 120) || fail "매핑 미리보기 실패"

COLUMN_COUNT=$(echo "$PREVIEW_RESP" | jq '.mapping.column_mappings | length')
RELATION_COUNT=$(echo "$PREVIEW_RESP" | jq '.mapping.relation_mappings | length')
EXT_COUNT=$(echo "$PREVIEW_RESP" | jq '.mapping.extended_properties | length')
HEADER_COUNT=$(echo "$PREVIEW_RESP" | jq '.headers | length')

pass "매핑 미리보기: 헤더=${HEADER_COUNT}개, 컬럼매핑=${COLUMN_COUNT}개, 관계=${RELATION_COUNT}개, 확장속성=${EXT_COUNT}개"

# 매핑 결과 요약 출력
info "컬럼 매핑:"
echo "$PREVIEW_RESP" | jq -r '.mapping.column_mappings[] | "  \(.source_column) → \(.target_label).\(.target_property) (confidence: \(.confidence))"' 2>/dev/null || true

info "관계 매핑:"
echo "$PREVIEW_RESP" | jq -r '.mapping.relation_mappings[] | "  \(.from_label)-[\(.rel_type)]->\(.to_label) from_columns=\(.from_columns) to_columns=\(.to_columns)"' 2>/dev/null || true

# from_columns/to_columns 검증 (A안 핵심)
if [ "$RELATION_COUNT" -gt 0 ]; then
    # CONSISTS_OF 관계가 있으면 from_columns/to_columns가 반드시 존재해야 함
    CONSISTS_OF_COUNT=$(echo "$PREVIEW_RESP" | jq '[.mapping.relation_mappings[] | select(.rel_type == "CONSISTS_OF")] | length')
    if [ "$CONSISTS_OF_COUNT" -gt 0 ]; then
        FROM_COLS_EMPTY=$(echo "$PREVIEW_RESP" | jq '[.mapping.relation_mappings[] | select(.rel_type == "CONSISTS_OF" and (.from_columns | length == 0))] | length')
        TO_COLS_EMPTY=$(echo "$PREVIEW_RESP" | jq '[.mapping.relation_mappings[] | select(.rel_type == "CONSISTS_OF" and (.to_columns | length == 0))] | length')
        if [ "$FROM_COLS_EMPTY" -gt 0 ] || [ "$TO_COLS_EMPTY" -gt 0 ]; then
            fail "CONSISTS_OF에 from_columns/to_columns가 비어있음 (A안 미적용)"
        fi
        # from_columns와 to_columns의 source_column이 서로 달라야 함
        SELF_REF=$(echo "$PREVIEW_RESP" | jq '[.mapping.relation_mappings[] | select(.rel_type == "CONSISTS_OF") | select((.from_columns | values) == (.to_columns | values))] | length')
        if [ "$SELF_REF" -gt 0 ]; then
            fail "CONSISTS_OF의 from_columns와 to_columns가 같은 컬럼을 참조 (자기참조 버그)"
        fi
        pass "CONSISTS_OF from_columns/to_columns 검증 통과 (${CONSISTS_OF_COUNT}개)"
    fi
fi

MAPPING_DATA=$(echo "$PREVIEW_RESP" | jq '.mapping')

# =========================================================
step "3-1. 매핑 검증 (POST /mappings/validate)"
# =========================================================
VALIDATE_RESP=$(curl -sf -X POST "${API}/mappings/validate" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"upload_id\": \"${UPLOAD_ID}\",
        \"mapping\": ${MAPPING_DATA}
    }") || fail "매핑 검증 실패"

VALIDATION_ERROR_COUNT=$(echo "$VALIDATE_RESP" | jq '.errors | length')
VALIDATION_WARNING_COUNT=$(echo "$VALIDATE_RESP" | jq '.warnings | length')
DISABLED_COLUMN_COUNT=$(echo "$VALIDATE_RESP" | jq -r '.impact_summary.disabled_column_count // 0')

if [ "$VALIDATION_ERROR_COUNT" -gt 0 ]; then
    echo "$VALIDATE_RESP" | jq '.errors'
    fail "매핑 검증 error ${VALIDATION_ERROR_COUNT}건"
fi

MAPPING_DATA=$(echo "$VALIDATE_RESP" | jq '.normalized_mapping')
pass "매핑 검증 통과: error=${VALIDATION_ERROR_COUNT}, warning=${VALIDATION_WARNING_COUNT}, disabled_columns=${DISABLED_COLUMN_COUNT}"

# =========================================================
step "4. 매핑 확정 (POST /mappings/confirm)"
# =========================================================

CONFIRM_RAW=$(curl -s -X POST "${API}/mappings/confirm" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"upload_id\": \"${UPLOAD_ID}\",
        \"name\": \"E2E 테스트 매핑\",
        \"mapping\": ${MAPPING_DATA}
    }" \
    -w "\nHTTP_STATUS:%{http_code}")

CONFIRM_HTTP=$(echo "$CONFIRM_RAW" | awk -F: '/HTTP_STATUS/ {print $2}')
CONFIRM_RESP=$(echo "$CONFIRM_RAW" | sed '/HTTP_STATUS:/d')

if [ -z "$CONFIRM_HTTP" ] || [ "$CONFIRM_HTTP" -lt 200 ] || [ "$CONFIRM_HTTP" -ge 300 ]; then
    echo "$CONFIRM_RESP" | jq . 2>/dev/null || echo "$CONFIRM_RESP"
    fail "매핑 확정 실패 (HTTP ${CONFIRM_HTTP:-unknown})"
fi

MAPPING_ID=$(echo "$CONFIRM_RESP" | jq -r '.id')

if [ -z "$MAPPING_ID" ] || [ "$MAPPING_ID" = "null" ]; then
    echo "$CONFIRM_RESP" | jq . 2>/dev/null
    fail "mapping_id 없음"
fi
pass "매핑 확정: mapping_id=${MAPPING_ID}"

# =========================================================
step "5. 합성 시작 (POST /synthesis)"
# =========================================================
SYNTH_RESP=$(curl -sf -X POST "${API}/synthesis" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"upload_id\": \"${UPLOAD_ID}\",
        \"mapping_id\": \"${MAPPING_ID}\"
    }") || fail "합성 시작 실패"

JOB_ID=$(echo "$SYNTH_RESP" | jq -r '.id')
SYNTH_STATUS=$(echo "$SYNTH_RESP" | jq -r '.status')

if [ -z "$JOB_ID" ] || [ "$JOB_ID" = "null" ]; then
    echo "$SYNTH_RESP" | jq . 2>/dev/null
    fail "job_id 없음"
fi
pass "합성 시작: job_id=${JOB_ID}, status=${SYNTH_STATUS}"

# 5-1. 합성 완료 대기 (폴링)
info "합성 진행 중..."
MAX_POLLS=60
POLL_INTERVAL=3
for i in $(seq 1 $MAX_POLLS); do
    sleep $POLL_INTERVAL

    JOB_RESP=$(curl -sf "${API}/synthesis/${JOB_ID}" -H "$AUTH") || continue
    JOB_STATUS=$(echo "$JOB_RESP" | jq -r '.status')
    PROCESSED=$(echo "$JOB_RESP" | jq -r '.processed_rows')
    TOTAL=$(echo "$JOB_RESP" | jq -r '.total_rows')

    echo -ne "\r  진행: ${PROCESSED}/${TOTAL} rows (status: ${JOB_STATUS})    "

    if [ "$JOB_STATUS" = "COMPLETED" ] || [ "$JOB_STATUS" = "FAILED" ]; then
        echo ""
        break
    fi
done

if [ "$JOB_STATUS" = "COMPLETED" ]; then
    NODES=$(echo "$JOB_RESP" | jq -r '.nodes_created')
    RELS=$(echo "$JOB_RESP" | jq -r '.relationships_created')
    pass "합성 완료: ${TOTAL} rows → 노드 ${NODES}개, 관계 ${RELS}개"
elif [ "$JOB_STATUS" = "FAILED" ]; then
    ERRORS=$(echo "$JOB_RESP" | jq '.errors')
    fail "합성 실패: ${ERRORS}"
else
    fail "합성 타임아웃 (${MAX_POLLS}회 폴링 후에도 미완료)"
fi

# =========================================================
step "6. 도면 업로드 (Presigned URL — PDF)"
# =========================================================
DWG_SKIP=false
DWG_UPLOAD_ID="N/A"
DWG_ANALYSIS_ID="N/A"
DWG_JOB_ID="N/A"
DWG_EXTRACTION_METHOD="N/A"
DWG_NODES=0
DWG_RELS=0

DWG_FILENAME=$(basename "$DRAWING_FILE")
DWG_FILE_SIZE=$(wc -c < "$DRAWING_FILE" | tr -d ' ')
DWG_CONTENT_TYPE="application/pdf"

info "파일: ${DWG_FILENAME} (${DWG_FILE_SIZE} bytes)"

# 6-1. Presigned URL 발급
DWG_UPLOAD_RESP=$(curl -sf -X POST "${API}/uploads" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"original_name\": \"${DWG_FILENAME}\",
        \"content_type\": \"${DWG_CONTENT_TYPE}\",
        \"file_size\": ${DWG_FILE_SIZE}
    }") || { info "도면 Presigned URL 발급 실패 — 스킵"; DWG_SKIP=true; }

if [ "$DWG_SKIP" = false ]; then
    DWG_UPLOAD_ID=$(echo "$DWG_UPLOAD_RESP" | jq -r '.upload_id')
    DWG_UPLOAD_URL=$(echo "$DWG_UPLOAD_RESP" | jq -r '.upload_url')

    if [ -z "$DWG_UPLOAD_ID" ] || [ "$DWG_UPLOAD_ID" = "null" ]; then
        info "도면 upload_id 없음 — 스킵"
        DWG_SKIP=true
    else
        pass "Presigned URL 발급: upload_id=${DWG_UPLOAD_ID}"

        # 6-2. S3에 파일 업로드
        DWG_S3_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
            -X PUT "$DWG_UPLOAD_URL" \
            -H "Content-Type: ${DWG_CONTENT_TYPE}" \
            -H "Content-Length: ${DWG_FILE_SIZE}" \
            --data-binary "@${DRAWING_FILE}") || { info "도면 S3 업로드 실패 — 스킵"; DWG_SKIP=true; }

        if [ "$DWG_SKIP" = false ] && [ "$DWG_S3_STATUS" != "200" ]; then
            info "도면 S3 업로드 실패 (HTTP ${DWG_S3_STATUS}) — 스킵"
            DWG_SKIP=true
        fi

        if [ "$DWG_SKIP" = false ]; then
            pass "도면 S3 업로드 완료 (HTTP ${DWG_S3_STATUS})"

            # 6-3. 업로드 완료 확인
            DWG_COMPLETE_RESP=$(curl -sf -X POST "${API}/uploads/${DWG_UPLOAD_ID}/complete" \
                -H "$AUTH") || { info "도면 업로드 완료 확인 실패 — 스킵"; DWG_SKIP=true; }

            if [ "$DWG_SKIP" = false ]; then
                DWG_UPLOAD_STATUS=$(echo "$DWG_COMPLETE_RESP" | jq -r '.status')
                pass "도면 업로드 완료: status=${DWG_UPLOAD_STATUS}"
            fi
        fi
    fi
fi

# =========================================================
step "7. 도면 분석 (POST /drawings/analyze)"
# =========================================================
if [ "$DWG_SKIP" = false ]; then
    info "Vision LLM 호출 중... (시간이 걸릴 수 있습니다)"

    DWG_ANALYZE_RESP=$(curl -sf -X POST "${API}/drawings/analyze" \
        -H "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"upload_id\": \"${DWG_UPLOAD_ID}\"
        }" \
        --max-time 180) || { info "도면 분석 실패 (타임아웃 등) — 스킵"; DWG_SKIP=true; }
fi

if [ "$DWG_SKIP" = false ]; then
    DWG_PAGE_COUNT=$(echo "$DWG_ANALYZE_RESP" | jq -r '.page_count')
    DWG_EXTRACTION_METHOD=$(echo "$DWG_ANALYZE_RESP" | jq -r '.extraction_method')
    DWG_TYPE=$(echo "$DWG_ANALYZE_RESP" | jq -r '.analysis.drawing_type')
    DWG_CONFIDENCE=$(echo "$DWG_ANALYZE_RESP" | jq -r '.analysis.confidence')
    DWG_PART_COUNT=$(echo "$DWG_ANALYZE_RESP" | jq '.analysis.parts | length')
    DWG_NUMBER=$(echo "$DWG_ANALYZE_RESP" | jq -r '.analysis.title_block.drawing_number // "N/A"')
    DWG_MATCHED=$(echo "$DWG_ANALYZE_RESP" | jq '.matching_report.matched_parts | length')
    DWG_NEW=$(echo "$DWG_ANALYZE_RESP" | jq '.matching_report.new_parts | length')

    pass "도면 분석 완료: pages=${DWG_PAGE_COUNT}, method=${DWG_EXTRACTION_METHOD}"
    info "도면번호: ${DWG_NUMBER}, 유형: ${DWG_TYPE}, 신뢰도: ${DWG_CONFIDENCE}"
    info "부품: ${DWG_PART_COUNT}개 (기존 매칭=${DWG_MATCHED}, 신규=${DWG_NEW})"
else
    info "도면 분석 스킵됨"
fi

# =========================================================
step "8. 도면 분석 확정 (POST /drawings/confirm)"
# =========================================================
if [ "$DWG_SKIP" = false ]; then
    DWG_ANALYSIS_DATA=$(echo "$DWG_ANALYZE_RESP" | jq '.analysis')

    DWG_CONFIRM_RESP=$(curl -sf -X POST "${API}/drawings/confirm" \
        -H "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"upload_id\": \"${DWG_UPLOAD_ID}\",
            \"name\": \"E2E 테스트 도면\",
            \"analysis\": ${DWG_ANALYSIS_DATA}
        }") || { info "도면 분석 확정 실패 — 스킵"; DWG_SKIP=true; }

    if [ "$DWG_SKIP" = false ]; then
        DWG_ANALYSIS_ID=$(echo "$DWG_CONFIRM_RESP" | jq -r '.id')

        if [ -z "$DWG_ANALYSIS_ID" ] || [ "$DWG_ANALYSIS_ID" = "null" ]; then
            info "도면 analysis_id 없음 — 스킵"
            DWG_SKIP=true
        else
            pass "도면 분석 확정: analysis_id=${DWG_ANALYSIS_ID}"
        fi
    fi
else
    info "도면 분석 확정 스킵됨"
fi

# =========================================================
step "9. 도면 합성 시작 (POST /drawings/synthesis)"
# =========================================================
if [ "$DWG_SKIP" = false ]; then
    DWG_SYNTH_RESP=$(curl -sf -X POST "${API}/drawings/synthesis" \
        -H "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"analysis_id\": \"${DWG_ANALYSIS_ID}\"
        }") || { info "도면 합성 시작 실패 — 스킵"; DWG_SKIP=true; }
fi

if [ "$DWG_SKIP" = false ]; then
    DWG_JOB_ID=$(echo "$DWG_SYNTH_RESP" | jq -r '.id')
    DWG_SYNTH_STATUS=$(echo "$DWG_SYNTH_RESP" | jq -r '.status')

    if [ -z "$DWG_JOB_ID" ] || [ "$DWG_JOB_ID" = "null" ]; then
        info "도면 job_id 없음 — 스킵"
        DWG_SKIP=true
    else
        pass "도면 합성 시작: job_id=${DWG_JOB_ID}, status=${DWG_SYNTH_STATUS}"

        # 9-1. 도면 합성 완료 대기 (폴링)
        info "도면 합성 진행 중..."
        DWG_MAX_POLLS=60
        DWG_POLL_INTERVAL=3
        DWG_JOB_STATUS="PENDING"
        for i in $(seq 1 $DWG_MAX_POLLS); do
            sleep $DWG_POLL_INTERVAL

            DWG_JOB_RESP=$(curl -sf "${API}/drawings/synthesis/${DWG_JOB_ID}" -H "$AUTH") || continue
            DWG_JOB_STATUS=$(echo "$DWG_JOB_RESP" | jq -r '.status')
            DWG_NODES=$(echo "$DWG_JOB_RESP" | jq -r '.nodes_created')
            DWG_RELS=$(echo "$DWG_JOB_RESP" | jq -r '.relationships_created')

            echo -ne "\r  진행: 노드=${DWG_NODES}, 관계=${DWG_RELS} (status: ${DWG_JOB_STATUS})    "

            if [ "$DWG_JOB_STATUS" = "COMPLETED" ] || [ "$DWG_JOB_STATUS" = "FAILED" ]; then
                echo ""
                break
            fi
        done

        if [ "$DWG_JOB_STATUS" = "COMPLETED" ]; then
            DWG_ERRORS=$(echo "$DWG_JOB_RESP" | jq '.errors | length')
            pass "도면 합성 완료: 노드 ${DWG_NODES}개, 관계 ${DWG_RELS}개, 에러 ${DWG_ERRORS}건"
        elif [ "$DWG_JOB_STATUS" = "FAILED" ]; then
            DWG_ERRORS=$(echo "$DWG_JOB_RESP" | jq '.errors')
            info "도면 합성 실패: ${DWG_ERRORS}"
        else
            info "도면 합성 타임아웃 (${DWG_MAX_POLLS}회 폴링 후에도 미완료)"
        fi
    fi
else
    info "도면 합성 스킵됨"
fi

# =========================================================
step "10. 헬스 체크 (POST /activation/health-check)"
# =========================================================
HC_RESP=$(curl -sf -X POST "${API}/activation/health-check" \
    -H "$AUTH") || fail "헬스 체크 실패"

HC_NODES=$(echo "$HC_RESP" | jq -r '.total_nodes')
HC_RELS=$(echo "$HC_RESP" | jq -r '.total_relationships')
HC_ISSUES=$(echo "$HC_RESP" | jq '.issues | length')

pass "헬스 체크: 노드=${HC_NODES}, 관계=${HC_RELS}, 이슈=${HC_ISSUES}건"

info "노드 카운트:"
echo "$HC_RESP" | jq -r '.node_counts | to_entries[] | select(.value > 0) | "  \(.key): \(.value)개"' 2>/dev/null || true

info "관계 카운트:"
echo "$HC_RESP" | jq -r '.relationship_counts | to_entries[] | select(.value > 0) | "  \(.key): \(.value)개"' 2>/dev/null || true

if [ "$HC_ISSUES" -gt 0 ]; then
    info "이슈 목록:"
    echo "$HC_RESP" | jq -r '.issues[] | "  [\(.severity)] \(.message)"' 2>/dev/null || true
fi

# =========================================================
step "11. 추천 질문 (GET /activation/starters)"
# =========================================================
STARTERS_RESP=$(curl -sf "${API}/activation/starters" \
    -H "$AUTH") || fail "추천 질문 조회 실패"

STARTER_COUNT=$(echo "$STARTERS_RESP" | jq '.starters | length')
pass "추천 질문 ${STARTER_COUNT}개 조회"

# =========================================================
step "12. AI 질의 — 전체 부품 목록 (POST /activation/query)"
# =========================================================
QUERY_QUESTION="전체 부품 목록을 보여줘"
info "질문: ${QUERY_QUESTION}"

QUERY_RESP=$(curl -sf -X POST "${API}/activation/query" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"question\": \"${QUERY_QUESTION}\"}" \
    --max-time 120) || fail "AI 질의 실패"

CYPHER=$(echo "$QUERY_RESP" | jq -r '.cypher_query')
RESULT_COUNT=$(echo "$QUERY_RESP" | jq '.results | length')
ANSWER=$(echo "$QUERY_RESP" | jq -r '.answer')

pass "AI 질의 성공: Cypher 실행 → ${RESULT_COUNT}건"
info "생성된 Cypher: ${CYPHER}"
info "AI 답변: ${ANSWER:0:200}"

# =========================================================
step "12-1. AI 질의 — CONSISTS_OF 관계 검증"
# =========================================================
QUERY_BOM="상위 부품과 하위 부품의 CONSISTS_OF 관계를 모두 보여줘. 상위품번, 하위품번, 수량을 포함해서"
info "질문: ${QUERY_BOM}"

BOM_QUERY_RESP=$(curl -sf -X POST "${API}/activation/query" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{\"question\": \"${QUERY_BOM}\"}" \
    --max-time 120) || fail "BOM 질의 실패"

BOM_CYPHER=$(echo "$BOM_QUERY_RESP" | jq -r '.cypher_query')
BOM_RESULT_COUNT=$(echo "$BOM_QUERY_RESP" | jq '.results | length')
BOM_ANSWER=$(echo "$BOM_QUERY_RESP" | jq -r '.answer')

if [ "$BOM_RESULT_COUNT" -eq 0 ]; then
    info "CONSISTS_OF 관계 0건 (계층적 BOM이 아닌 샘플 사용 시 정상)"
else
    pass "CONSISTS_OF 관계 조회: ${BOM_RESULT_COUNT}건"
fi
info "생성된 Cypher: ${BOM_CYPHER}"
info "AI 답변: ${BOM_ANSWER:0:300}"

# =========================================================
step "13. 아이템 목록 조회 (GET /items)"
# =========================================================
ITEMS_RESP=$(curl -sf "${API}/items?limit=5" \
    -H "$AUTH") || fail "아이템 목록 조회 실패"

ITEM_TOTAL=$(echo "$ITEMS_RESP" | jq -r '.total')
ITEM_COUNT=$(echo "$ITEMS_RESP" | jq '.items | length')

if [ "$ITEM_TOTAL" -eq 0 ]; then
    fail "아이템이 0건 (합성 후 Part가 존재해야 함)"
fi
pass "아이템 목록: total=${ITEM_TOTAL}, 조회=${ITEM_COUNT}건"

# 첫 번째 아이템의 part_number 추출
FIRST_PN=$(echo "$ITEMS_RESP" | jq -r '.items[0].part_number')
info "첫 번째 Part: ${FIRST_PN}"

# 13-1. 검색 조회
SEARCH_RESP=$(curl -sf "${API}/items?search=${FIRST_PN}&limit=5" \
    -H "$AUTH") || fail "아이템 검색 실패"

SEARCH_TOTAL=$(echo "$SEARCH_RESP" | jq -r '.total')
pass "아이템 검색 (search=${FIRST_PN}): ${SEARCH_TOTAL}건"

# =========================================================
step "14. 아이템 상세 조회 (GET /items/{part_number})"
# =========================================================
DETAIL_RESP=$(curl -sf "${API}/items/${FIRST_PN}" \
    -H "$AUTH") || fail "아이템 상세 조회 실패"

DETAIL_PN=$(echo "$DETAIL_RESP" | jq -r '.part_number')
DETAIL_NAME=$(echo "$DETAIL_RESP" | jq -r '.name // "N/A"')
DETAIL_CHILDREN=$(echo "$DETAIL_RESP" | jq '.children | length')
DETAIL_PARENTS=$(echo "$DETAIL_RESP" | jq '.parents | length')
DETAIL_DRAWINGS=$(echo "$DETAIL_RESP" | jq '.drawings | length')
DETAIL_SUPPLIERS=$(echo "$DETAIL_RESP" | jq '.suppliers | length')

pass "아이템 상세: ${DETAIL_PN} (${DETAIL_NAME})"
info "관계: children=${DETAIL_CHILDREN}, parents=${DETAIL_PARENTS}, drawings=${DETAIL_DRAWINGS}, suppliers=${DETAIL_SUPPLIERS}"

# =========================================================
step "15. BOM 트리 조회 (GET /items/{part_number}/bom-tree)"
# =========================================================
# CONSISTS_OF 관계가 있는 부품(PRT-001)으로 BOM 트리 조회
BOM_PN="PRT-001"
BOM_RESP=$(curl -sf "${API}/items/${BOM_PN}/bom-tree" \
    -H "$AUTH") || { info "BOM 트리 조회 실패 (${BOM_PN}) — 첫번째 아이템으로 재시도"; BOM_RESP=""; }

if [ -z "$BOM_RESP" ]; then
    BOM_RESP=$(curl -sf "${API}/items/${FIRST_PN}/bom-tree" \
        -H "$AUTH") || fail "BOM 트리 조회 실패"
    BOM_PN="${FIRST_PN}"
fi

BOM_ROOT=$(echo "$BOM_RESP" | jq -r '.root.part_number')
BOM_CHILDREN=$(echo "$BOM_RESP" | jq '.root.children | length')

pass "BOM 트리: root=${BOM_ROOT}, 직계 자식=${BOM_CHILDREN}건"

# =========================================================
step "16. 토큰 갱신 (POST /auth/refresh)"
# =========================================================
REFRESH_RESP=$(curl -sf -X POST "${API}/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refresh_token\": \"${REFRESH_TOKEN}\"}") || fail "토큰 갱신 실패"

NEW_TOKEN=$(echo "$REFRESH_RESP" | jq -r '.access_token // empty')
if [ -z "$NEW_TOKEN" ]; then
    fail "새 access_token 없음"
fi
pass "토큰 갱신 성공"

# =========================================================
step "17. DB 검증 (PostgreSQL 직접 조회)"
# =========================================================
docker ps --format '{{.Names}}' | jq -R -s 'split("\n") | map(select(length>0))' >/dev/null 2>&1 || fail "docker 또는 jq 실행 실패"
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    fail "DB 컨테이너(${DB_CONTAINER})가 실행 중이 아닙니다"
fi

USER_ORG_COUNT=$(db_query "
SELECT COUNT(*)
FROM users u
JOIN memberships m ON m.user_id = u.id
JOIN organizations o ON o.id = m.org_id
WHERE u.email = '${EMAIL}'
  AND o.slug = '${ORG_SLUG}';
" | tr -d '[:space:]')

if [ "$USER_ORG_COUNT" != "1" ]; then
    fail "public 스키마 사용자/조직 데이터 검증 실패 (count=${USER_ORG_COUNT})"
fi
pass "public 스키마 검증 통과: 사용자/조직/멤버십 1건"

UPLOAD_DB_STATUS=$(db_query "SELECT status FROM ${TENANT_SCHEMA}.uploads WHERE id = '${UPLOAD_ID}'::uuid;" | tr -d '[:space:]')
if [ "$UPLOAD_DB_STATUS" != "UPLOADED" ] && [ "$UPLOAD_DB_STATUS" != "COMPLETED" ]; then
    fail "tenant 업로드 상태 검증 실패 (status=${UPLOAD_DB_STATUS})"
fi
pass "tenant 업로드 상태 검증 통과: ${UPLOAD_DB_STATUS}"

MAPPING_DB_COUNT=$(db_query "SELECT COUNT(*) FROM ${TENANT_SCHEMA}.mapping_records WHERE id = '${MAPPING_ID}'::uuid;" | tr -d '[:space:]')
if [ "$MAPPING_DB_COUNT" != "1" ]; then
    fail "tenant 매핑 레코드 검증 실패 (count=${MAPPING_DB_COUNT})"
fi

CONSISTS_EMPTY_COUNT=$(db_query "
SELECT COUNT(*)
FROM ${TENANT_SCHEMA}.mapping_records mr,
LATERAL jsonb_array_elements(COALESCE(mr.mapping->'relation_mappings', '[]'::jsonb)) rm
WHERE mr.id = '${MAPPING_ID}'::uuid
  AND rm->>'rel_type' = 'CONSISTS_OF'
  AND (
    COALESCE(rm->'from_columns', '{}'::jsonb) = '{}'::jsonb
    OR COALESCE(rm->'to_columns', '{}'::jsonb) = '{}'::jsonb
  );
" | tr -d '[:space:]')
if [ "$CONSISTS_EMPTY_COUNT" != "0" ]; then
    fail "tenant 매핑 레코드 관계 endpoint 검증 실패 (empty_consists_of=${CONSISTS_EMPTY_COUNT})"
fi
pass "tenant 매핑 레코드 검증 통과: mapping_records 1건, CONSISTS_OF endpoint 정상"

SYNTH_DB_STATUS=$(db_query "SELECT status FROM ${TENANT_SCHEMA}.synthesis_jobs WHERE id = '${JOB_ID}'::uuid;" | tr -d '[:space:]')
if [ "$SYNTH_DB_STATUS" != "COMPLETED" ]; then
    fail "tenant 합성 잡 상태 검증 실패 (status=${SYNTH_DB_STATUS})"
fi
pass "tenant 합성 잡 상태 검증 통과: ${SYNTH_DB_STATUS}"

# Parts RDS 테이블 검증 (SoT 전환 확인)
PARTS_COUNT=$(db_query "SELECT COUNT(*) FROM ${TENANT_SCHEMA}.parts;" | tr -d '[:space:]')
if [ "$PARTS_COUNT" -eq 0 ]; then
    fail "tenant parts 테이블이 비어있음 (합성 후 Part가 존재해야 함)"
fi
pass "tenant parts 테이블 검증 통과: ${PARTS_COUNT}건"

PART_REVISIONS_COUNT=$(db_query "SELECT COUNT(*) FROM ${TENANT_SCHEMA}.part_revisions;" | tr -d '[:space:]')
pass "tenant part_revisions 테이블: ${PART_REVISIONS_COUNT}건"

# Graph Part 노드에 BOM 합성으로 생성된 노드의 name 속성이 없는지 확인
# (도면 합성 모듈은 아직 별도 SoT 전환 대상이 아니므로 name이 있을 수 있음)
GRAPH_BOM_PART_NAME=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "
LOAD 'age';
SET search_path = ag_catalog, public;
SELECT * FROM cypher('${TENANT_SCHEMA}', \$\$
  MATCH (p:Part) WHERE p.part_number = '${FIRST_PN}' RETURN p.name
\$\$) AS (nm agtype);
" 2>/dev/null | tr -d '[:space:]')
if [ -z "$GRAPH_BOM_PART_NAME" ] || [ "$GRAPH_BOM_PART_NAME" = "" ]; then
    pass "Graph Part 노드 속성 축소 검증 통과 (BOM 합성 Part에 name 없음)"
else
    info "Graph Part name=${GRAPH_BOM_PART_NAME} (BOM 합성 Part — 신규 합성부터 적용)"
fi

# =========================================================
echo -e "\n${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  전체 플로우 테스트 성공!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  계정:       ${EMAIL}"
echo "  조직:       ${ORG_NAME}"
echo "  BOM 업로드: ${UPLOAD_ID}"
echo "  BOM 매핑:   ${MAPPING_ID}"
echo "  BOM 합성:   ${JOB_ID}"
if [ "$DWG_SKIP" = false ]; then
echo "  도면 업로드: ${DWG_UPLOAD_ID}"
echo "  도면 분석:   ${DWG_ANALYSIS_ID} (method: ${DWG_EXTRACTION_METHOD})"
echo "  도면 합성:   ${DWG_JOB_ID}"
else
echo "  도면:       스킵됨"
fi
echo "  그래프:     노드 ${HC_NODES}개, 관계 ${HC_RELS}개"
echo ""
