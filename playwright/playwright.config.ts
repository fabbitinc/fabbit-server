import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : 1,
  timeout: 90_000,
  expect: {
    timeout: 10_000,
  },
  reporter: [["list"], ["html", { open: "never", outputFolder: "report" }]],
  use: {
    baseURL: process.env.API_BASE_URL ?? "http://127.0.0.1:8080",
    extraHTTPHeaders: {
      "X-Test-Suite": "playwright-api",
    },
    trace: "retain-on-failure",
  },
});
