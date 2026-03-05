import fs from "node:fs";
import path from "node:path";

const ROOT_DIR = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..");
const OPENAPI_PATH = path.resolve(ROOT_DIR, "..", "openapi.json");
const MATRIX_PATH = path.resolve(ROOT_DIR, "openapi", "endpoint-matrix.json");
const EXCLUDE_PATH = path.resolve(ROOT_DIR, "openapi", "exclusions.json");

const HTTP_METHODS = new Set(["GET", "POST", "PUT", "PATCH", "DELETE"]);

function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf-8"));
}

const openapi = loadJson(OPENAPI_PATH);
const matrix = loadJson(MATRIX_PATH);
const exclusions = loadJson(EXCLUDE_PATH);

const openapiTargets = new Set();
const exclusionSet = new Set(
  exclusions.map((item) => `${item.method.toUpperCase()} ${item.path}`),
);

for (const [pathTemplate, pathItem] of Object.entries(openapi.paths ?? {})) {
  for (const [methodRaw] of Object.entries(pathItem)) {
    const method = methodRaw.toUpperCase();
    if (!HTTP_METHODS.has(method)) {
      continue;
    }
    const key = `${method} ${pathTemplate}`;
    if (!exclusionSet.has(key)) {
      openapiTargets.add(key);
    }
  }
}

const matrixKeys = new Set(matrix.operations.map((op) => op.key));

const missingInMatrix = [...openapiTargets].filter((key) => !matrixKeys.has(key));
const extraInMatrix = [...matrixKeys].filter((key) => !openapiTargets.has(key));

if (missingInMatrix.length > 0 || extraInMatrix.length > 0) {
  console.error(`[coverage] matrix mismatch detected`);
  if (missingInMatrix.length > 0) {
    console.error(`[coverage] missing in matrix (${missingInMatrix.length})`);
    for (const key of missingInMatrix) {
      console.error(`  - ${key}`);
    }
  }
  if (extraInMatrix.length > 0) {
    console.error(`[coverage] extra in matrix (${extraInMatrix.length})`);
    for (const key of extraInMatrix) {
      console.error(`  - ${key}`);
    }
  }
  process.exit(1);
}

console.log(`[coverage] OK target=${openapiTargets.size}`);
