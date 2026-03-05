import { expect, test } from "@playwright/test";

import { assertErrorFormat, callApi, expectNoServerError } from "../../helpers/http";
import {
  findOperation,
  loadOpenApi,
  pickJsonSchemaFromResponse,
  type OpenApiOperation,
} from "../../helpers/openapi";
import { validateAgainstSchema } from "../../helpers/schema";
import { bootstrapSession, type TestSession } from "../../helpers/session";

const RUN_OPTIONAL_EXCLUDED = process.env.PW_RUN_EXCLUDED === "1";

const openapi = loadOpenApi();
const previewOperation = findOperation(openapi, "POST", "/api/v1/mappings/preview");
const activationHealthCheckOperation = findOperation(
  openapi,
  "POST",
  "/api/v1/activation/health-check",
);
const activationQueryOperation = findOperation(openapi, "POST", "/api/v1/activation/query");
const activationStartersOperation = findOperation(openapi, "GET", "/api/v1/activation/starters");

let session: TestSession;

function assertSuccessSchema(
  operation: OpenApiOperation,
  status: number,
  payload: unknown,
  context: string,
): void {
  const schema = pickJsonSchemaFromResponse(openapi, operation, status);
  if (!schema || payload == null) {
    return;
  }
  validateAgainstSchema(openapi, payload, schema, { path: context });
}

test.describe.configure({ mode: "serial" });

test.describe("제외 엔드포인트 선택 실행", () => {
  test.skip(!RUN_OPTIONAL_EXCLUDED, "PW_RUN_EXCLUDED=1 설정 시에만 실행합니다.");

  test.beforeAll(async ({ request }) => {
    session = await bootstrapSession(request);
  });

  test("POST /api/v1/mappings/preview contract", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/mappings/preview", {
      token: session.owner.accessToken,
      data: {
        file_id: session.resources.fileId,
      },
    });

    expectNoServerError(result, "POST /api/v1/mappings/preview contract");
    expect(result.status).toBe(200);
    assertSuccessSchema(
      previewOperation,
      result.status,
      result.json,
      "POST /api/v1/mappings/preview response",
    );
  });

  test("POST /api/v1/mappings/preview failure", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/mappings/preview", {
      token: session.owner.accessToken,
      data: {},
    });

    expectNoServerError(result, "POST /api/v1/mappings/preview failure");
    expect([400, 422]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "POST /api/v1/mappings/preview failure");
    }
  });

  test("POST /api/v1/mappings/preview auth", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/mappings/preview", {
      data: {
        file_id: session.resources.fileId,
      },
    });

    expect([401, 403]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "POST /api/v1/mappings/preview auth");
    }
  });

  test("POST /api/v1/activation/health-check contract", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/activation/health-check", {
      token: session.owner.accessToken,
    });

    expectNoServerError(result, "POST /api/v1/activation/health-check contract");
    expect(result.status).toBe(200);
    assertSuccessSchema(
      activationHealthCheckOperation,
      result.status,
      result.json,
      "POST /api/v1/activation/health-check response",
    );
  });

  test("POST /api/v1/activation/health-check auth", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/activation/health-check");

    expect([401, 403]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "POST /api/v1/activation/health-check auth");
    }
  });

  test("POST /api/v1/activation/query contract", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/activation/query", {
      token: session.owner.accessToken,
      data: {
        question: "현재 그래프 기준으로 주요 부품 관계를 요약해줘.",
      },
    });

    expectNoServerError(result, "POST /api/v1/activation/query contract");
    expect(result.status).toBe(200);
    assertSuccessSchema(
      activationQueryOperation,
      result.status,
      result.json,
      "POST /api/v1/activation/query response",
    );
  });

  test("POST /api/v1/activation/query failure", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/activation/query", {
      token: session.owner.accessToken,
      data: {},
    });

    expectNoServerError(result, "POST /api/v1/activation/query failure");
    expect([400, 422]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "POST /api/v1/activation/query failure");
    }
  });

  test("POST /api/v1/activation/query auth", async ({ request }) => {
    const result = await callApi(request, "POST", "/api/v1/activation/query", {
      data: {
        question: "test",
      },
    });

    expect([401, 403]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "POST /api/v1/activation/query auth");
    }
  });

  test("GET /api/v1/activation/starters contract", async ({ request }) => {
    const result = await callApi(request, "GET", "/api/v1/activation/starters", {
      token: session.owner.accessToken,
    });

    expectNoServerError(result, "GET /api/v1/activation/starters contract");
    expect(result.status).toBe(200);
    assertSuccessSchema(
      activationStartersOperation,
      result.status,
      result.json,
      "GET /api/v1/activation/starters response",
    );
  });

  test("GET /api/v1/activation/starters auth", async ({ request }) => {
    const result = await callApi(request, "GET", "/api/v1/activation/starters");

    expect([401, 403]).toContain(result.status);
    if (result.json) {
      assertErrorFormat(result.json, "GET /api/v1/activation/starters auth");
    }
  });
});
