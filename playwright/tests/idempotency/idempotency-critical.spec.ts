import { expect, test } from "@playwright/test";

import { callApi, expectNoServerError } from "../../helpers/http";
import { bootstrapSession } from "../../helpers/session";

test.describe("멱등성/중복 요청", () => {
  test("동일 synthesis 시작 요청 재시도 시 서버 오류 없이 처리되어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const payload = {
      mapping_id: session.resources.mappingId,
      uploads: [{ file_id: session.resources.fileId }],
    };

    const first = await callApi(request, "POST", "/api/v1/synthesis", {
      token: session.owner.accessToken,
      data: payload,
    });
    const second = await callApi(request, "POST", "/api/v1/synthesis", {
      token: session.owner.accessToken,
      data: payload,
    });

    expectNoServerError(first, "synthesis first");
    expectNoServerError(second, "synthesis second");
    expect([200, 400, 409, 422]).toContain(second.status);
  });

  test("동일 part link 요청을 반복해도 서버 오류가 발생하면 안 된다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const payload = {
      part_ids: [session.resources.partId],
    };

    const first = await callApi(
      request,
      "POST",
      `/api/v1/projects/${session.resources.projectId}/parts`,
      {
        token: session.owner.accessToken,
        data: payload,
      },
    );

    const second = await callApi(
      request,
      "POST",
      `/api/v1/projects/${session.resources.projectId}/parts`,
      {
        token: session.owner.accessToken,
        data: payload,
      },
    );

    expectNoServerError(first, "project parts link first");
    expectNoServerError(second, "project parts link second");
    expect([200, 400, 409, 422]).toContain(second.status);
  });
});
