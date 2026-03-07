import type { OperationMatrixItem } from "./openapi";

const RELAXED_UNDOCUMENTED_KEYS = new Set<string>([
  "POST /api/v1/auth/send-verification",
  "POST /api/v1/auth/verify-email",
  "POST /api/v1/auth/register",
  "POST /api/v1/auth/accept-invitation",
  "POST /api/v1/auth/login",
  "POST /api/v1/auth/logout",
  "PUT /api/v1/users/me/password",
  "POST /api/v1/files/upload/{fileId}/complete",
]);

export function isRelaxedOperation(operation: OperationMatrixItem): boolean {
  return RELAXED_UNDOCUMENTED_KEYS.has(operation.key);
}

export function isDocumentedStatus(
  operation: OperationMatrixItem,
  status: number,
): boolean {
  const code = String(status);
  if (operation.responseCodes.includes(code)) {
    return true;
  }
  return operation.responseCodes.includes("DEFAULT");
}

export function allowedUndocumentedStatus(
  operation: OperationMatrixItem,
  status: number,
): boolean {
  if (!isRelaxedOperation(operation)) {
    return false;
  }

  // 비즈니스 검증 에러는 문서보다 넓게 발생할 수 있어 4xx를 허용한다.
  return status >= 400 && status < 500;
}
