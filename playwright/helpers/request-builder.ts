import { randomUUID } from "node:crypto";

import type {
  OpenApiDocument,
  OpenApiOperation,
  OpenApiParameter,
  OpenApiSchema,
  OperationMatrixItem,
} from "./openapi";
import { listOperationParameters, resolveRequestBody, resolveSchema } from "./openapi";
import type { TestSession } from "./session";

export interface BuiltRequest {
  url: string;
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
}

export type RequestMode = "contract" | "failure" | "auth" | "idempotency";

function stringValueByHint(nameHint: string, session: TestSession): string {
  const n = nameHint.toLowerCase();

  if (n.includes("email")) {
    return `pw_${session.suffix}_${Math.floor(Math.random() * 100000)}@example.com`;
  }
  if (n.includes("password")) {
    return "TestPass1234!";
  }
  if (n === "slug") {
    return session.owner.slug;
  }
  if (n.includes("token")) {
    if (n.includes("refresh")) {
      return session.owner.refreshToken;
    }
    if (n === "token") {
      return session.resources.invitationToken;
    }
    return `token-${session.suffix}`;
  }
  if (n.includes("code")) {
    return "123456";
  }
  if (n.includes("color")) {
    return "#1E88E5";
  }
  if (n.includes("url")) {
    return "https://example.com";
  }
  if (n.includes("name")) {
    return `pw-${nameHint}-${session.suffix}-${randomUUID().slice(0, 8)}`;
  }
  if (n.includes("title")) {
    return `PW ${session.suffix} title`;
  }
  if (n.includes("description")) {
    return `PW ${session.suffix} description`;
  }
  if (n.includes("search")) {
    return "PW";
  }
  if (n === "cr_state") {
    return "DRAFT";
  }
  if (n.includes("state")) {
    return "OPEN";
  }
  if (n.includes("type")) {
    return "ISSUE";
  }
  if (n.includes("owner_type")) {
    return "part";
  }

  return `pw-${session.suffix}`;
}

function contextValueByKey(
  key: string,
  session: TestSession,
  pathTemplate: string,
): unknown {
  const k = key.toLowerCase();

  if (k === "project_id") return session.resources.projectId;
  if (k === "team_id") return session.resources.teamId;
  if (k === "default_owner_team_id") return session.resources.teamId;
  if (k === "default_owner_id") return session.owner.userId;
  if (k === "part_id") return session.resources.partId;
  if (k === "file_id") {
    if (pathTemplate.includes("profile-image")) {
      return session.resources.imageFileId;
    }
    return session.resources.fileId;
  }
  if (k === "mapping_id") return session.resources.mappingId;
  if (k === "label_id") return session.resources.labelId;
  if (k === "user_id") return session.member.userId;
  if (k === "batch_id") return session.resources.synthesisBatchId;
  if (k === "job_id") return session.resources.synthesisJobId;
  if (k === "notification_id") return session.resources.notificationId;
  if (k === "invitation_id") return session.resources.invitationId;
  if (k === "category") return session.resources.category;

  if (k === "issue_number") {
    if (pathTemplate.startsWith("/api/v1/changes/")) {
      return session.resources.changeNumber;
    }
    return session.resources.issueNumber;
  }

  if (k === "comment_id") {
    if (pathTemplate.startsWith("/api/v1/changes/")) {
      return session.resources.changeCommentId;
    }
    return session.resources.issueCommentId;
  }

  if (k.endsWith("_id")) return randomUUID();
  if (k.endsWith("_ids")) return [randomUUID()];

  return undefined;
}

function safePathValueForStateChange(param: OpenApiParameter): string | number {
  const schema = param.schema;
  if (schema?.type === "integer" || schema?.type === "number") {
    return 999999;
  }
  if (schema?.format === "uuid") {
    return randomUUID();
  }
  if (schema?.type === "string") {
    return `missing-${Date.now()}`;
  }
  return randomUUID();
}

function generateValueFromSchema(
  openapi: OpenApiDocument,
  rawSchema: OpenApiSchema,
  nameHint: string,
  session: TestSession,
  pathTemplate: string,
  depth = 0,
): unknown {
  if (depth > 10) {
    return null;
  }

  const schema = resolveSchema(openapi, rawSchema);

  if (schema.default !== undefined) {
    return schema.default;
  }

  if (schema.enum?.length) {
    return schema.enum[0];
  }

  if (schema.oneOf?.length) {
    return generateValueFromSchema(
      openapi,
      schema.oneOf[0],
      nameHint,
      session,
      pathTemplate,
      depth + 1,
    );
  }

  if (schema.anyOf?.length) {
    return generateValueFromSchema(
      openapi,
      schema.anyOf[0],
      nameHint,
      session,
      pathTemplate,
      depth + 1,
    );
  }

  if (schema.allOf?.length) {
    const merged: Record<string, unknown> = {};
    for (const child of schema.allOf) {
      const childValue = generateValueFromSchema(
        openapi,
        child,
        nameHint,
        session,
        pathTemplate,
        depth + 1,
      );
      if (childValue && typeof childValue === "object" && !Array.isArray(childValue)) {
        Object.assign(merged, childValue);
      }
    }
    return merged;
  }

  const contextValue = contextValueByKey(nameHint, session, pathTemplate);
  if (contextValue !== undefined) {
    return contextValue;
  }

  switch (schema.type) {
    case "string": {
      if (schema.format === "uuid") {
        return randomUUID();
      }
      return stringValueByHint(nameHint, session);
    }
    case "integer": {
      return Math.max(1, Number(schema.minimum ?? 1));
    }
    case "number": {
      return Number(schema.minimum ?? 1);
    }
    case "boolean": {
      return false;
    }
    case "array": {
      if (!schema.items) {
        return [];
      }
      return [
        generateValueFromSchema(
          openapi,
          schema.items,
          nameHint.replace(/s$/, ""),
          session,
          pathTemplate,
          depth + 1,
        ),
      ];
    }
    case "object": {
      const result: Record<string, unknown> = {};
      const required = schema.required ?? [];
      const properties = schema.properties ?? {};

      const keys = required.length > 0 ? required : Object.keys(properties).slice(0, 3);
      for (const key of keys) {
        const propSchema = properties[key];
        if (!propSchema) {
          continue;
        }
        result[key] = generateValueFromSchema(
          openapi,
          propSchema,
          key,
          session,
          pathTemplate,
          depth + 1,
        );
      }

      // TipTap 문서는 빈 content 배열로 고정하면 안정적이다.
      if (nameHint.toLowerCase().includes("body") && "type" in properties && "content" in properties) {
        result.type = "doc";
        result.content = [];
      }

      return result;
    }
    default: {
      if (schema.properties) {
        return generateValueFromSchema(
          openapi,
          { ...schema, type: "object" },
          nameHint,
          session,
          pathTemplate,
          depth + 1,
        );
      }
      return null;
    }
  }
}

function invalidValueForParameter(param: OpenApiParameter): string {
  const schema = param.schema;
  if (!schema) {
    return "invalid";
  }

  const resolvedType = schema.type;
  const format = schema.format;

  if (resolvedType === "integer" || resolvedType === "number") {
    return "NaN";
  }
  if (format === "uuid") {
    return "not-a-uuid";
  }

  return "invalid";
}

function applyPathParams(
  pathTemplate: string,
  pathParams: Record<string, unknown>,
): string {
  let result = pathTemplate;
  for (const [key, value] of Object.entries(pathParams)) {
    result = result.replace(`{${key}}`, encodeURIComponent(String(value)));
  }
  return result;
}

function buildQueryParams(
  openapi: OpenApiDocument,
  operation: OpenApiOperation,
  operationMeta: OperationMatrixItem,
  session: TestSession,
  mode: RequestMode,
  parameters: OpenApiParameter[],
): Record<string, unknown> {
  const queryParams = parameters.filter((p) => p.in === "query");
  const result: Record<string, unknown> = {};

  if (mode === "failure") {
    const required = queryParams.find((p) => p.required);
    if (required) {
      for (const param of queryParams) {
        if (param.name === required.name) {
          continue;
        }
        if (!param.schema) {
          continue;
        }
        result[param.name] = generateValueFromSchema(
          openapi,
          param.schema,
          param.name,
          session,
          operationMeta.path,
        );
      }
      return result;
    }
  }

  for (const param of queryParams) {
    if (!param.required && mode !== "contract") {
      continue;
    }
    if (!param.schema) {
      continue;
    }

    let value = generateValueFromSchema(
      openapi,
      param.schema,
      param.name,
      session,
      operationMeta.path,
    );

    if (mode === "failure") {
      value = invalidValueForParameter(param);
    }

    result[param.name] = value;
  }

  // 로그인/사이트 엔드포인트는 Origin이 중요하므로 query는 최소로 유지한다.
  void operation;

  return result;
}

function buildRequestBody(
  openapi: OpenApiDocument,
  operation: OpenApiOperation,
  operationMeta: OperationMatrixItem,
  session: TestSession,
  mode: RequestMode,
): unknown {
  if (!operation.requestBody) {
    return undefined;
  }

  const body = resolveRequestBody(openapi, operation.requestBody);
  const media =
    body.content?.["application/json"] ??
    body.content?.["application/problem+json"] ??
    Object.values(body.content ?? {})[0];

  if (!media?.schema) {
    return undefined;
  }

  if (mode === "failure") {
    if (body.required) {
      return {};
    }
    return undefined;
  }

  return generateValueFromSchema(
    openapi,
    media.schema,
    operationMeta.operationId ?? "body",
    session,
    operationMeta.path,
  );
}

function buildSpecialHeaders(operationMeta: OperationMatrixItem, session: TestSession): Record<string, string> {
  const headers: Record<string, string> = {};

  if (operationMeta.path === "/api/v1/auth/site" || operationMeta.path === "/api/v1/auth/login") {
    headers.Origin = `http://${session.owner.slug}.lvh.me`;
  }

  if (operationMeta.path === "/api/v1/notifications/stream") {
    headers.Accept = "text/event-stream";
  }

  return headers;
}

export function buildOperationRequest(
  openapi: OpenApiDocument,
  operationMeta: OperationMatrixItem,
  operation: OpenApiOperation,
  session: TestSession,
  mode: RequestMode,
): BuiltRequest {
  const allParameters = listOperationParameters(
    openapi,
    operationMeta.method,
    operationMeta.path,
  );
  const pathParameters = allParameters.filter((p) => p.in === "path");

  const pathParamValues: Record<string, unknown> = {};
  for (const param of pathParameters) {
    if (mode === "failure") {
      pathParamValues[param.name] = invalidValueForParameter(param);
      continue;
    }

    if (operationMeta.stateChanging && (mode === "contract" || mode === "idempotency")) {
      // 계약/멱등 테스트에서는 파괴적 부작용을 피하기 위해 안전한 미존재 식별자를 우선 사용한다.
      pathParamValues[param.name] = safePathValueForStateChange(param);
      continue;
    }

    const contextValue = contextValueByKey(param.name, session, operationMeta.path);
    if (contextValue !== undefined) {
      pathParamValues[param.name] = contextValue;
      continue;
    }

    if (param.schema) {
      pathParamValues[param.name] = generateValueFromSchema(
        openapi,
        param.schema,
        param.name,
        session,
        operationMeta.path,
      );
    } else {
      pathParamValues[param.name] = randomUUID();
    }
  }

  const url = applyPathParams(operationMeta.path, pathParamValues);
  const params = buildQueryParams(
    openapi,
    operation,
    operationMeta,
    session,
    mode,
    allParameters,
  );

  const data = buildRequestBody(openapi, operation, operationMeta, session, mode);
  const headers = buildSpecialHeaders(operationMeta, session);

  return {
    url,
    params: Object.keys(params).length > 0 ? params : undefined,
    data,
    headers: Object.keys(headers).length > 0 ? headers : undefined,
  };
}
