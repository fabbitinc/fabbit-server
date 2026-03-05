import { randomUUID } from "node:crypto";

import { expect, test } from "@playwright/test";

import { callApi, expectNoServerError } from "../../helpers/http";
import { bootstrapSession } from "../../helpers/session";

test.describe("핵심 실패 시나리오", () => {
  test("배치 합성에서 일부 파일 실패가 발생해도 전체 요청은 부분 성공으로 반환되어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const result = await callApi(request, "POST", "/api/v1/synthesis", {
      token: session.owner.accessToken,
      data: {
        mapping_id: session.resources.mappingId,
        uploads: [
          { file_id: session.resources.fileId },
          { file_id: randomUUID() },
        ],
      },
    });

    expect(result.status, result.text).toBe(200);
    expectNoServerError(result, "partial synthesis");

    const json = result.json as Record<string, unknown>;
    expect(Number(json.requested_count ?? 0)).toBe(2);
    expect(Number(json.accepted_count ?? 0)).toBeGreaterThanOrEqual(1);

    const failed = (json.failed as Array<Record<string, unknown>>) ?? [];
    expect(failed.length).toBeGreaterThanOrEqual(1);
  });

  test("실패 원인 제거 후 재시도 요청이 정상 수락되어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const retry = await callApi(request, "POST", "/api/v1/synthesis", {
      token: session.owner.accessToken,
      data: {
        mapping_id: session.resources.mappingId,
        uploads: [{ file_id: session.resources.fileId }],
      },
    });

    expect(retry.status, retry.text).toBe(200);
    const json = retry.json as Record<string, unknown>;
    expect(Number(json.accepted_count ?? 0)).toBeGreaterThanOrEqual(1);
  });
});
