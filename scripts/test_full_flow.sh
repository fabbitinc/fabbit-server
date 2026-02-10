#!/usr/bin/env bash
# =========================================================
#  Fabbit 전체 플로우 E2E 테스트 스크립트
#  signup → upload → mapping → synthesis → activation
# =========================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000}"
API="${BASE_URL}/api/v1"
SAMPLE_FILE="${SAMPLE_FILE:-sample/Arduino_Uno_R3_From_Scratch.csv}"
HEADER_ROW="${HEADER_ROW:-3}"

# 랜덤 이메일 생성 (중복 방지)
RANDOM_SUFFIX=$(date +%s)
EMAIL="test_${RANDOM_SUFFIX}@example.com"
PASSWORD="TestPass1234"
ORG_NAME="TestOrg_${RANDOM_SUFFIX}"
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

# 서버 헬스체크
step "0. 서버 헬스체크"
HEALTH=$(curl -sf "${BASE_URL}/health" 2>/dev/null || echo "")
if [ -z "$HEALTH" ]; then
    fail "서버가 응답하지 않습니다 (${BASE_URL})"
fi
pass "서버 정상 동작"

# =========================================================
step "1. 회원가입 (POST /auth/signup)"
# =========================================================
SIGNUP_RESP=$(curl -sf -X POST "${API}/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"${EMAIL}\",
        \"password\": \"${PASSWORD}\",
        \"full_name\": \"${FULL_NAME}\",
        \"org_name\": \"${ORG_NAME}\"
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
pass "내 정보: email=${ME_EMAIL}, org=${ME_ORG}"

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
        \"upload_id\": \"${UPLOAD_ID}\",
        \"header_row\": ${HEADER_ROW}
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

# =========================================================
step "4. 매핑 확정 (POST /mappings/confirm)"
# =========================================================
MAPPING_DATA=$(echo "$PREVIEW_RESP" | jq '.mapping')
HEADERS_DATA=$(echo "$PREVIEW_RESP" | jq '.headers')

CONFIRM_RESP=$(curl -sf -X POST "${API}/mappings/confirm" \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"upload_id\": \"${UPLOAD_ID}\",
        \"name\": \"E2E 테스트 매핑\",
        \"header_row\": ${HEADER_ROW},
        \"mapping\": ${MAPPING_DATA}
    }") || fail "매핑 확정 실패"

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
        \"mapping_id\": \"${MAPPING_ID}\",
        \"header_row\": ${HEADER_ROW}
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
step "6. 헬스 체크 (POST /activation/health-check)"
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
step "7. 추천 질문 (GET /activation/starters)"
# =========================================================
STARTERS_RESP=$(curl -sf "${API}/activation/starters" \
    -H "$AUTH") || fail "추천 질문 조회 실패"

STARTER_COUNT=$(echo "$STARTERS_RESP" | jq '.starters | length')
pass "추천 질문 ${STARTER_COUNT}개 조회"

# =========================================================
step "8. AI 질의 (POST /activation/query)"
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
step "9. 토큰 갱신 (POST /auth/refresh)"
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
echo -e "\n${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  전체 플로우 테스트 성공!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  계정:     ${EMAIL}"
echo "  조직:     ${ORG_NAME}"
echo "  업로드:   ${UPLOAD_ID}"
echo "  매핑:     ${MAPPING_ID}"
echo "  합성 작업: ${JOB_ID}"
echo "  그래프:   노드 ${HC_NODES}개, 관계 ${HC_RELS}개"
echo ""
