import fs from "node:fs";
import path from "node:path";

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface OperationMatrixItem {
  key: string;
  method: HttpMethod;
  path: string;
  operationId: string | null;
  tags: string[];
  protected: boolean;
  hasPathParams: boolean;
  hasRequestBody: boolean;
  stateChanging: boolean;
  responseCodes: string[];
  primarySuccessCode: string | null;
  requiredCases: {
    contract: boolean;
    failure: boolean;
    auth: boolean;
    idempotency: boolean;
  };
}

export interface EndpointMatrix {
  generated_at: string;
  source_openapi: string;
  total_operations: number;
  excluded_operations: Array<{ method: string; path: string; reason?: string }>;
  target_operations: number;
  operations: OperationMatrixItem[];
}

export interface OpenApiDocument {
  openapi: string;
  security?: Array<Record<string, unknown>>;
  paths: Record<string, OpenApiPathItem>;
  components?: {
    schemas?: Record<string, OpenApiSchema>;
    parameters?: Record<string, OpenApiParameter>;
    requestBodies?: Record<string, OpenApiRequestBody>;
    responses?: Record<string, OpenApiResponse>;
  };
}

export interface OpenApiPathItem {
  parameters?: OpenApiParameterOrRef[];
  get?: OpenApiOperation;
  post?: OpenApiOperation;
  put?: OpenApiOperation;
  patch?: OpenApiOperation;
  delete?: OpenApiOperation;
}

export interface OpenApiOperation {
  operationId?: string;
  tags?: string[];
  summary?: string;
  security?: Array<Record<string, unknown>>;
  parameters?: OpenApiParameterOrRef[];
  requestBody?: OpenApiRequestBodyOrRef;
  responses?: Record<string, OpenApiResponseOrRef>;
}

export interface OpenApiParameter {
  name: string;
  in: "path" | "query" | "header" | "cookie";
  required?: boolean;
  schema?: OpenApiSchema;
}

export interface OpenApiRequestBody {
  required?: boolean;
  content?: Record<string, { schema?: OpenApiSchema }>;
}

export interface OpenApiResponse {
  description?: string;
  content?: Record<string, { schema?: OpenApiSchema }>;
}

export interface OpenApiSchema {
  $ref?: string;
  type?: string;
  format?: string;
  enum?: unknown[];
  oneOf?: OpenApiSchema[];
  anyOf?: OpenApiSchema[];
  allOf?: OpenApiSchema[];
  items?: OpenApiSchema;
  properties?: Record<string, OpenApiSchema>;
  required?: string[];
  additionalProperties?: boolean | OpenApiSchema;
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  default?: unknown;
  nullable?: boolean;
  example?: unknown;
}

export type OpenApiParameterOrRef = OpenApiParameter | { $ref: string };
export type OpenApiRequestBodyOrRef = OpenApiRequestBody | { $ref: string };
export type OpenApiResponseOrRef = OpenApiResponse | { $ref: string };

const ROOT_DIR = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..");
const OPENAPI_PATH = path.resolve(ROOT_DIR, "..", "openapi.json");
const MATRIX_PATH = path.resolve(ROOT_DIR, "openapi", "endpoint-matrix.json");

function loadJsonFile<T>(targetPath: string): T {
  return JSON.parse(fs.readFileSync(targetPath, "utf-8")) as T;
}

export function loadOpenApi(): OpenApiDocument {
  return loadJsonFile<OpenApiDocument>(OPENAPI_PATH);
}

export function loadEndpointMatrix(): EndpointMatrix {
  return loadJsonFile<EndpointMatrix>(MATRIX_PATH);
}

export function findOperation(
  openapi: OpenApiDocument,
  method: HttpMethod,
  pathTemplate: string,
): OpenApiOperation {
  const pathItem = openapi.paths[pathTemplate];
  if (!pathItem) {
    throw new Error(`OpenAPI path를 찾을 수 없습니다: ${pathTemplate}`);
  }
  const op = pathItem[method.toLowerCase()];
  if (!op) {
    throw new Error(`OpenAPI operation을 찾을 수 없습니다: ${method} ${pathTemplate}`);
  }
  return op;
}

export function listOperationParameters(
  openapi: OpenApiDocument,
  method: HttpMethod,
  pathTemplate: string,
): OpenApiParameter[] {
  const pathItem = openapi.paths[pathTemplate];
  if (!pathItem) {
    return [];
  }

  const operation = pathItem[method.toLowerCase() as keyof OpenApiPathItem] as OpenApiOperation | undefined;
  const rawParameters = [...(pathItem.parameters ?? []), ...(operation?.parameters ?? [])];
  return rawParameters.map((param) => resolveParameter(openapi, param));
}

function derefRefPath<T>(openapi: OpenApiDocument, ref: string): T {
  if (!ref.startsWith("#/")) {
    throw new Error(`지원하지 않는 ref 형식: ${ref}`);
  }
  const parts = ref.slice(2).split("/");
  let cursor: unknown = openapi;
  for (const part of parts) {
    if (!cursor || typeof cursor !== "object" || !(part in (cursor as Record<string, unknown>))) {
      throw new Error(`ref 해석 실패: ${ref}`);
    }
    cursor = (cursor as Record<string, unknown>)[part];
  }
  return cursor as T;
}

export function resolveParameter(
  openapi: OpenApiDocument,
  parameter: OpenApiParameterOrRef,
): OpenApiParameter {
  if ("$ref" in parameter) {
    return derefRefPath<OpenApiParameter>(openapi, parameter.$ref);
  }
  return parameter;
}

export function resolveRequestBody(
  openapi: OpenApiDocument,
  requestBody: OpenApiRequestBodyOrRef,
): OpenApiRequestBody {
  if ("$ref" in requestBody) {
    return derefRefPath<OpenApiRequestBody>(openapi, requestBody.$ref);
  }
  return requestBody;
}

export function resolveResponse(
  openapi: OpenApiDocument,
  response: OpenApiResponseOrRef,
): OpenApiResponse {
  if ("$ref" in response) {
    return derefRefPath<OpenApiResponse>(openapi, response.$ref);
  }
  return response;
}

export function resolveSchema(
  openapi: OpenApiDocument,
  schema: OpenApiSchema,
): OpenApiSchema {
  if (!schema.$ref) {
    return schema;
  }
  return derefRefPath<OpenApiSchema>(openapi, schema.$ref);
}

export function pickJsonSchemaFromResponse(
  openapi: OpenApiDocument,
  operation: OpenApiOperation,
  statusCode: number,
): OpenApiSchema | null {
  const responses = operation.responses ?? {};
  const codeKey = String(statusCode);
  const candidate = responses[codeKey] ?? responses.default;
  if (!candidate) {
    return null;
  }
  const response = resolveResponse(openapi, candidate);
  const media = response.content?.["application/json"] ?? response.content?.["application/problem+json"];
  if (!media?.schema) {
    return null;
  }
  return media.schema;
}
