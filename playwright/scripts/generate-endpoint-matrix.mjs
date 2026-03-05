import fs from "node:fs";
import path from "node:path";

const ROOT_DIR = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..");
const OPENAPI_PATH = path.resolve(ROOT_DIR, "..", "openapi.json");
const EXCLUDE_PATH = path.resolve(ROOT_DIR, "openapi", "exclusions.json");
const OUTPUT_PATH = path.resolve(ROOT_DIR, "openapi", "endpoint-matrix.json");

const HTTP_METHODS = new Set(["GET", "POST", "PUT", "PATCH", "DELETE"]);

function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf-8"));
}

function normalizeStatusCodes(responses) {
  return Object.keys(responses ?? {})
    .map((code) => code.toUpperCase())
    .filter((code) => code === "DEFAULT" || /^\d{3}$/.test(code))
    .sort();
}

function pickPrimarySuccess(codes) {
  const success = codes.find((code) => code.startsWith("2"));
  return success ?? null;
}

function isProtected(op, rootOpenApi) {
  const security = op.security ?? rootOpenApi.security;
  return Array.isArray(security) && security.length > 0;
}

function hasPathParams(pathTemplate) {
  return pathTemplate.includes("{") && pathTemplate.includes("}");
}

function hasRequestBody(op) {
  return Boolean(op.requestBody);
}

function isStateChanging(method) {
  return method !== "GET";
}

const openapi = loadJson(OPENAPI_PATH);
const exclusions = loadJson(EXCLUDE_PATH);
const exclusionSet = new Set(
  exclusions.map((item) => `${item.method.toUpperCase()} ${item.path}`),
);

const operations = [];
let totalOperations = 0;

for (const [pathTemplate, pathItem] of Object.entries(openapi.paths ?? {})) {
  for (const [methodRaw, op] of Object.entries(pathItem)) {
    const method = methodRaw.toUpperCase();
    if (!HTTP_METHODS.has(method)) {
      continue;
    }
    totalOperations += 1;
    const key = `${method} ${pathTemplate}`;
    if (exclusionSet.has(key)) {
      continue;
    }

    const responseCodes = normalizeStatusCodes(op.responses);
    operations.push({
      key,
      method,
      path: pathTemplate,
      operationId: op.operationId ?? null,
      tags: op.tags ?? [],
      protected: isProtected(op, openapi),
      hasPathParams: hasPathParams(pathTemplate),
      hasRequestBody: hasRequestBody(op),
      stateChanging: isStateChanging(method),
      responseCodes,
      primarySuccessCode: pickPrimarySuccess(responseCodes),
      requiredCases: {
        contract: true,
        failure: true,
        auth: isProtected(op, openapi),
        idempotency: isStateChanging(method),
      },
    });
  }
}

operations.sort((a, b) => {
  if (a.path === b.path) {
    return a.method.localeCompare(b.method);
  }
  return a.path.localeCompare(b.path);
});

const output = {
  generated_at: new Date().toISOString(),
  source_openapi: path.relative(ROOT_DIR, OPENAPI_PATH),
  total_operations: totalOperations,
  excluded_operations: exclusions,
  target_operations: operations.length,
  operations,
};

fs.writeFileSync(OUTPUT_PATH, `${JSON.stringify(output, null, 2)}\n`, "utf-8");

console.log(
  `[matrix] total=${totalOperations} excluded=${exclusions.length} target=${operations.length} -> ${path.relative(process.cwd(), OUTPUT_PATH)}`,
);
