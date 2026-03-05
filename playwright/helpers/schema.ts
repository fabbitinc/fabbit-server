import { expect } from "@playwright/test";

import type { OpenApiDocument, OpenApiSchema } from "./openapi";
import { resolveSchema } from "./openapi";

interface ValidateOptions {
  path?: string;
  depth?: number;
}

const MAX_DEPTH = 20;

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function validatePrimitive(value: unknown, schemaType: string, path: string): void {
  switch (schemaType) {
    case "string":
      expect(typeof value, `${path}는 string이어야 합니다`).toBe("string");
      break;
    case "integer":
      expect(typeof value, `${path}는 integer이어야 합니다`).toBe("number");
      expect(Number.isInteger(value), `${path}는 integer이어야 합니다`).toBeTruthy();
      break;
    case "number":
      expect(typeof value, `${path}는 number이어야 합니다`).toBe("number");
      break;
    case "boolean":
      expect(typeof value, `${path}는 boolean이어야 합니다`).toBe("boolean");
      break;
    case "array":
      expect(Array.isArray(value), `${path}는 array이어야 합니다`).toBeTruthy();
      break;
    case "object":
      expect(isObject(value), `${path}는 object여야 합니다`).toBeTruthy();
      break;
    default:
      break;
  }
}

function validateAnyOfOneOf(
  openapi: OpenApiDocument,
  value: unknown,
  schemas: OpenApiSchema[],
  path: string,
  depth: number,
): void {
  let matched = 0;
  for (const candidate of schemas) {
    try {
      validateAgainstSchema(openapi, value, candidate, { path, depth: depth + 1 });
      matched += 1;
    } catch {
      // 후보가 실패해도 다음 후보를 시도한다.
    }
  }
  expect(matched > 0, `${path}는 oneOf/anyOf 조건 중 하나를 만족해야 합니다`).toBeTruthy();
}

function schemaAllowsNull(schema: OpenApiSchema): boolean {
  if (schema.nullable) {
    return true;
  }
  if (schema.type === "null") {
    return true;
  }
  if (schema.anyOf?.some((item) => schemaAllowsNull(item))) {
    return true;
  }
  if (schema.oneOf?.some((item) => schemaAllowsNull(item))) {
    return true;
  }
  if (schema.allOf?.some((item) => schemaAllowsNull(item))) {
    return true;
  }
  return false;
}

export function validateAgainstSchema(
  openapi: OpenApiDocument,
  value: unknown,
  rawSchema: OpenApiSchema,
  options: ValidateOptions = {},
): void {
  const path = options.path ?? "$";
  const depth = options.depth ?? 0;

  if (depth > MAX_DEPTH) {
    return;
  }

  const schema = resolveSchema(openapi, rawSchema);

  if (value === null) {
    if (schemaAllowsNull(schema)) {
      return;
    }
    throw new Error(`${path}는 null이 될 수 없습니다`);
  }

  if (schema.oneOf?.length) {
    validateAnyOfOneOf(openapi, value, schema.oneOf, path, depth);
    return;
  }

  if (schema.anyOf?.length) {
    validateAnyOfOneOf(openapi, value, schema.anyOf, path, depth);
    return;
  }

  if (schema.allOf?.length) {
    for (const s of schema.allOf) {
      validateAgainstSchema(openapi, value, s, { path, depth: depth + 1 });
    }
    return;
  }

  if (schema.enum?.length) {
    const allowed = schema.enum as unknown[];
    expect(allowed.includes(value), `${path}는 enum 값이어야 합니다`).toBeTruthy();
    return;
  }

  if (schema.type) {
    validatePrimitive(value, schema.type, path);
  }

  if (schema.type === "array" && Array.isArray(value) && schema.items) {
    for (let i = 0; i < value.length; i += 1) {
      validateAgainstSchema(openapi, value[i], schema.items, {
        path: `${path}[${i}]`,
        depth: depth + 1,
      });
    }
    return;
  }

  if ((schema.type === "object" || schema.properties || schema.required) && isObject(value)) {
    const required = schema.required ?? [];
    for (const key of required) {
      expect(key in value, `${path}.${key} 필수 필드가 없습니다`).toBeTruthy();
    }

    const properties = schema.properties ?? {};
    for (const [key, childSchema] of Object.entries(properties)) {
      if (!(key in value)) {
        continue;
      }
      validateAgainstSchema(openapi, value[key], childSchema, {
        path: `${path}.${key}`,
        depth: depth + 1,
      });
    }

    if (schema.additionalProperties && isObject(schema.additionalProperties)) {
      for (const [k, v] of Object.entries(value)) {
        if (k in properties) {
          continue;
        }
        validateAgainstSchema(openapi, v, schema.additionalProperties, {
          path: `${path}.${k}`,
          depth: depth + 1,
        });
      }
    }
  }
}
