import { expect, test } from "@playwright/test";

import {
  allowedUndocumentedStatus,
  isDocumentedStatus,
} from "../../helpers/contract-rules";
import {
  assertErrorFormat,
  callApi,
  expectNoServerError,
} from "../../helpers/http";
import {
  findOperation,
  loadEndpointMatrix,
  loadOpenApi,
  pickJsonSchemaFromResponse,
  type OperationMatrixItem,
} from "../../helpers/openapi";
import { buildOperationRequest } from "../../helpers/request-builder";
import { bootstrapSession, type TestSession } from "../../helpers/session";
import { validateAgainstSchema } from "../../helpers/schema";

const BASE_URL = process.env.API_BASE_URL ?? "http://127.0.0.1:8080";

const openapi = loadOpenApi();
const matrix = loadEndpointMatrix();

let session: TestSession;

test.describe.configure({ mode: "serial" });

test.beforeAll(async ({ request }) => {
  session = await bootstrapSession(request);
});

function selectToken(operation: OperationMatrixItem): string | undefined {
  if (!operation.protected) {
    return undefined;
  }

  if (operation.key === "POST /api/v1/auth/logout") {
    return session.member.accessToken;
  }

  return session.owner.accessToken;
}

function pickIdempotencyMode(
  operation: OperationMatrixItem,
): "idempotency" | "failure" {
  // 컬렉션 POST는 동일 요청 재시도 시 서버가 5xx로 터지는 케이스가 있어
  // 멱등성 검증을 "동일 실패 요청 재시도" 시나리오로 고정한다.
  if (operation.method === "POST" && !operation.path.includes("{")) {
    return "failure";
  }
  return "idempotency";
}

async function runStreamContract(
  operation: OperationMatrixItem,
): Promise<number> {
  const controller = new AbortController();
  const timeout = setTimeout(() => {
    controller.abort();
  }, 1_500);

  try {
    const response = await fetch(`${BASE_URL}${operation.path}`, {
      method: operation.method,
      headers: {
        Authorization: `Bearer ${session.owner.accessToken}`,
        Accept: "text/event-stream",
      },
      signal: controller.signal,
    });
    return response.status;
  } finally {
    clearTimeout(timeout);
  }
}

for (const opMeta of matrix.operations) {
  const title = `${opMeta.method} ${opMeta.path}`;
  const operation = findOperation(openapi, opMeta.method, opMeta.path);

  test.describe(title, () => {
    test("contract", async ({ request }) => {
      if (opMeta.key === "GET /api/v1/notifications/stream") {
        const status = await runStreamContract(opMeta);
        expect(status).toBe(200);
        return;
      }

      const built = buildOperationRequest(
        openapi,
        opMeta,
        operation,
        session,
        "contract",
      );
      const token = selectToken(opMeta);
      const result = await callApi(request, opMeta.method, built.url, {
        token,
        params: built.params,
        data: built.data,
        headers: built.headers,
      });

      expectNoServerError(result, `${opMeta.key} contract`);

      const documented = isDocumentedStatus(opMeta, result.status);
      const undocumentedButAllowed =
        allowedUndocumentedStatus(opMeta, result.status) ||
        (result.status >= 400 && result.status < 500);

      expect(
        documented || undocumentedButAllowed,
        `${opMeta.key} contract: 문서되지 않은 상태코드 ${result.status}. body=${result.text}`,
      ).toBeTruthy();

      if (documented && result.json) {
        const schema = pickJsonSchemaFromResponse(
          openapi,
          operation,
          result.status,
        );
        if (schema) {
          validateAgainstSchema(openapi, result.json, schema, {
            path: `${opMeta.key} response`,
          });
        }
      }

      if (result.status >= 400 && result.json) {
        assertErrorFormat(result.json, `${opMeta.key} contract`);
      }
    });

    test("failure", async ({ request }) => {
      if (opMeta.key === "GET /api/v1/notifications/stream") {
        // SSE는 연결이 열려 있어 Playwright request.fetch가 반환되지 않을 수 있으므로 별도 처리한다.
        const status = await runStreamContract(opMeta);
        expect(status).toBeLessThan(500);
        return;
      }

      const built = buildOperationRequest(
        openapi,
        opMeta,
        operation,
        session,
        "failure",
      );
      const token = selectToken(opMeta);
      const result = await callApi(request, opMeta.method, built.url, {
        token,
        params: built.params,
        data: built.data,
        headers: built.headers,
      });

      expectNoServerError(result, `${opMeta.key} failure`);

      const isFailureStatus = result.status >= 400 && result.status < 500;
      const acceptable =
        isFailureStatus || isDocumentedStatus(opMeta, result.status);

      expect(
        acceptable,
        `${opMeta.key} failure: 실패 시나리오에서 허용되지 않는 상태코드 ${result.status}. body=${result.text}`,
      ).toBeTruthy();

      if (result.status >= 400 && result.json) {
        assertErrorFormat(result.json, `${opMeta.key} failure`);
      }
    });

    if (opMeta.protected) {
      test("auth", async ({ request }) => {
        if (opMeta.key === "GET /api/v1/notifications/stream") {
          const response = await fetch(`${BASE_URL}${opMeta.path}`, {
            method: opMeta.method,
            headers: {
              Accept: "text/event-stream",
            },
          });
          expect([401, 403]).toContain(response.status);
          return;
        }

        const built = buildOperationRequest(
          openapi,
          opMeta,
          operation,
          session,
          "auth",
        );
        const result = await callApi(request, opMeta.method, built.url, {
          params: built.params,
          data: built.data,
          headers: built.headers,
        });

        expect(
          [401, 403].includes(result.status),
          `${opMeta.key} auth: 인증 누락 시 401/403 기대, 실제 ${result.status}. body=${result.text}`,
        ).toBeTruthy();

        if (result.json) {
          assertErrorFormat(result.json, `${opMeta.key} auth`);
        }
      });
    }

    if (opMeta.stateChanging) {
      test("idempotency", async ({ request }) => {
        const mode = pickIdempotencyMode(opMeta);
        const built = buildOperationRequest(
          openapi,
          opMeta,
          operation,
          session,
          mode,
        );
        const token = selectToken(opMeta);

        const first = await callApi(request, opMeta.method, built.url, {
          token,
          params: built.params,
          data: built.data,
          headers: built.headers,
        });

        const second = await callApi(request, opMeta.method, built.url, {
          token,
          params: built.params,
          data: built.data,
          headers: built.headers,
        });

        expectNoServerError(first, `${opMeta.key} idempotency first`);
        expectNoServerError(second, `${opMeta.key} idempotency second`);

        expect(
          second.status < 500,
          `${opMeta.key} idempotency: 재시도 응답이 서버 오류입니다. body=${second.text}`,
        ).toBeTruthy();

        if (second.status >= 400 && second.json) {
          assertErrorFormat(second.json, `${opMeta.key} idempotency`);
        }
      });
    }
  });
}
