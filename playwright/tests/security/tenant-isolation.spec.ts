import { expect, test } from "@playwright/test";

import { callApi } from "../../helpers/http";
import { bootstrapSession, registerIsolatedAccount } from "../../helpers/session";

test.describe("권한/테넌트 격리", () => {
  test("다른 테넌트 토큰으로는 기존 테넌트 리소스 접근이 차단되어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);
    const isolated = await registerIsolatedAccount(request, `iso-${Date.now()}`);

    const ownAccess = await callApi(
      request,
      "GET",
      `/api/v1/parts/${session.resources.partId}`,
      {
        token: session.owner.accessToken,
      },
    );
    expect(ownAccess.status, ownAccess.text).toBe(200);

    const crossPart = await callApi(
      request,
      "GET",
      `/api/v1/parts/${session.resources.partId}`,
      {
        token: isolated.accessToken,
      },
    );
    expect([403, 404]).toContain(crossPart.status);

    const crossProject = await callApi(
      request,
      "GET",
      `/api/v1/projects/${session.resources.projectId}`,
      {
        token: isolated.accessToken,
      },
    );
    expect([403, 404]).toContain(crossProject.status);
  });
});
