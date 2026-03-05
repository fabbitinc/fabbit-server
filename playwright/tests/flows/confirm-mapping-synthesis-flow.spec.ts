import { expect, test } from "@playwright/test";

import { callApi } from "../../helpers/http";
import { bootstrapSession } from "../../helpers/session";

test.describe("confirm_mapping + synthesis 핵심 플로우", () => {
  test("scope_stress_bom.csv 기반으로 Part 생성이 완료되어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const mappingResult = await callApi(request, "GET", `/api/v1/mappings/${session.resources.mappingId}`, {
      token: session.owner.accessToken,
    });
    expect(mappingResult.status, mappingResult.text).toBe(200);

    const mappingJson = mappingResult.json as Record<string, unknown>;
    expect(mappingJson.id).toBe(session.resources.mappingId);
    expect(mappingJson.scope).toBeTruthy();

    const synthesisResult = await callApi(request, "GET", `/api/v1/synthesis/${session.resources.synthesisJobId}`, {
      token: session.owner.accessToken,
    });
    expect(synthesisResult.status, synthesisResult.text).toBe(200);

    const synthesisJson = synthesisResult.json as Record<string, unknown>;
    expect(["COMPLETED", "COMPLETED_WITH_ERRORS"]).toContain(synthesisJson.status);
    expect(Number(synthesisJson.total_rows ?? 0)).toBeGreaterThan(0);

    const partsResult = await callApi(request, "GET", "/api/v1/parts", {
      token: session.owner.accessToken,
      params: {
        limit: 100,
      },
    });
    expect(partsResult.status, partsResult.text).toBe(200);

    const partsJson = partsResult.json as Record<string, unknown>;
    const items = (partsJson.items as Array<Record<string, unknown>>) ?? [];
    const partNumbers = new Set(items.map((item) => String(item.part_number ?? "")));

    expect(partNumbers.has("WIDE-ROOT")).toBeTruthy();
    expect(partNumbers.has("WIDE-C01")).toBeTruthy();
    expect(partNumbers.has("WIDE-C02")).toBeTruthy();
  });
});
