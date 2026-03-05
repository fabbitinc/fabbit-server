import { performance } from "node:perf_hooks";

import { expect, test } from "@playwright/test";

import { callApi } from "../../helpers/http";
import { bootstrapSession } from "../../helpers/session";

interface Sample {
  ms: number;
  status: number;
}

function percentile(values: number[], p: number): number {
  if (values.length === 0) {
    return 0;
  }
  const sorted = [...values].sort((a, b) => a - b);
  const idx = Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length));
  return sorted[idx];
}

test.describe("API 스트레스", () => {
  test("GET /parts 버스트 부하에서 오류율과 p95를 측정한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const total = Number(process.env.PW_STRESS_READ_TOTAL ?? 30);
    const concurrency = Number(process.env.PW_STRESS_READ_CONCURRENCY ?? 10);

    const samples: Sample[] = [];

    let cursor = 0;
    async function worker(): Promise<void> {
      while (cursor < total) {
        const index = cursor;
        cursor += 1;

        const start = performance.now();
        const result = await callApi(request, "GET", "/api/v1/parts", {
          token: session.owner.accessToken,
          params: {
            offset: index % 5,
            limit: 20,
          },
        });
        samples.push({
          ms: performance.now() - start,
          status: result.status,
        });
      }
    }

    await Promise.all(Array.from({ length: concurrency }, () => worker()));

    const statusOk = samples.filter((s) => s.status < 500);
    const errorRate = 1 - statusOk.length / samples.length;
    const p95 = percentile(samples.map((s) => s.ms), 95);

    expect(errorRate, `read error_rate=${errorRate}`).toBeLessThan(0.05);
    expect(p95, `read p95=${p95}`).toBeLessThan(2_500);
  });

  test("POST /synthesis 반복 요청에서 5xx가 없어야 한다", async ({ request }) => {
    const session = await bootstrapSession(request);

    const total = Number(process.env.PW_STRESS_WRITE_TOTAL ?? 10);
    const samples: Sample[] = [];

    for (let i = 0; i < total; i += 1) {
      const start = performance.now();
      const result = await callApi(request, "POST", "/api/v1/synthesis", {
        token: session.owner.accessToken,
        data: {
          mapping_id: session.resources.mappingId,
          uploads: [{ file_id: session.resources.fileId }],
        },
      });
      samples.push({
        ms: performance.now() - start,
        status: result.status,
      });
    }

    const serverErrors = samples.filter((s) => s.status >= 500);
    const p95 = percentile(samples.map((s) => s.ms), 95);

    expect(serverErrors.length, `write 5xx count=${serverErrors.length}`).toBe(0);
    expect(p95, `write p95=${p95}`).toBeLessThan(5_000);
  });
});
