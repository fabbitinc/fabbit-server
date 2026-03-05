import { expect, type APIRequestContext, type APIResponse } from "@playwright/test";

export interface HttpCallOptions {
  token?: string;
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
  timeout?: number;
}

export interface HttpResult {
  response: APIResponse;
  status: number;
  json: unknown;
  text: string;
}

function buildHeaders(options: HttpCallOptions): Record<string, string> {
  const headers: Record<string, string> = {
    ...(options.headers ?? {}),
  };
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }
  return headers;
}

export async function callApi(
  request: APIRequestContext,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  url: string,
  options: HttpCallOptions = {},
): Promise<HttpResult> {
  const response = await request.fetch(url, {
    method,
    params: options.params,
    data: options.data,
    headers: buildHeaders(options),
    timeout: options.timeout,
  });

  const status = response.status();
  const contentType = response.headers()["content-type"] ?? "";
  let text = "";
  let json: unknown = null;

  // SSE는 연결이 지속되므로 본문 전체를 읽으려 하면 테스트가 멈출 수 있다.
  if (!contentType.includes("text/event-stream")) {
    text = await response.text();

    if (contentType.includes("application/json") || text.startsWith("{") || text.startsWith("[")) {
      try {
        json = text ? JSON.parse(text) : null;
      } catch {
        json = null;
      }
    }
  }

  return {
    response,
    status,
    json,
    text,
  };
}

export function expectNoServerError(result: HttpResult, context: string): void {
  expect(result.status, `${context}: 5xx 응답은 허용되지 않습니다. body=${result.text}`).toBeLessThan(500);
}

export function assertErrorFormat(payload: unknown, context: string): void {
  expect(payload, `${context}: 에러 응답은 JSON 객체여야 합니다`).toBeTruthy();
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error(`${context}: 에러 응답 형식이 객체가 아닙니다`);
  }

  const obj = payload as Record<string, unknown>;

  const hasFormat =
    "detail" in obj ||
    ("message" in obj && "code" in obj) ||
    ("error" in obj && typeof obj.error === "object") ||
    ("errors" in obj && Array.isArray(obj.errors));

  expect(hasFormat, `${context}: 표준 에러 포맷(detail/message+code/error/errors) 중 하나가 필요합니다`).toBeTruthy();
}
