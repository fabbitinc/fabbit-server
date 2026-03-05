import fs from "node:fs";
import path from "node:path";
import { randomUUID } from "node:crypto";

import { expect, type APIRequestContext } from "@playwright/test";

import { callApi } from "./http";

const PASSWORD = "TestPass1234!";

export interface TestAccount {
  email: string;
  password: string;
  slug: string;
  accessToken: string;
  refreshToken: string;
  userId: string;
  orgId: string;
}

export interface TestResources {
  fileId: string;
  imageFileId: string;
  mappingId: string;
  synthesisJobId: string;
  synthesisBatchId: string;
  projectId: string;
  teamId: string;
  labelId: string;
  issueNumber: number;
  issueId: string;
  changeNumber: number;
  changeId: string;
  issueCommentId: string;
  changeCommentId: string;
  partId: string;
  asmPartId: string;
  category: string;
  invitationId: string;
  invitationToken: string;
  notificationId: string;
}

export interface TestSession {
  suffix: string;
  owner: TestAccount;
  member: TestAccount;
  resources: TestResources;
}

interface MailMessageSummary {
  ID: string;
  Subject?: string;
  To?: Array<{ Address?: string }>;
  Created?: string;
}

interface MailListResponse {
  messages?: MailMessageSummary[];
}

const ROOT_DIR = path.resolve(
  path.dirname(new URL(import.meta.url).pathname),
  "..",
);
const CSV_PATH = path.resolve(
  ROOT_DIR,
  "fixtures",
  "csv",
  "scope_stress_bom.csv",
);
const MAPPING_PATH = path.resolve(
  ROOT_DIR,
  "fixtures",
  "mappings",
  "scope_stress_bom.mapping.json",
);
const TINY_PNG_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2NuxkAAAAASUVORK5CYII=";

let cachedSessionPromise: Promise<TestSession> | null = null;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function createSuffix(): string {
  const ts = Date.now();
  const short = randomUUID().slice(0, 8);
  return `${ts}${short}`;
}

function parseJsonSafe<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

async function getMailMessages(): Promise<MailMessageSummary[]> {
  const response = await fetch("http://127.0.0.1:8025/api/v1/messages");
  if (!response.ok) {
    throw new Error(`MailHog 조회 실패: ${response.status}`);
  }
  const payload = (await response.json()) as MailListResponse;
  return payload.messages ?? [];
}

async function getMailMessageDetail(id: string): Promise<string> {
  const response = await fetch(`http://127.0.0.1:8025/api/v1/message/${id}`);
  if (!response.ok) {
    throw new Error(`MailHog 상세 조회 실패: ${response.status}`);
  }
  const data = await response.json();
  return JSON.stringify(data);
}

async function pollMailBody(
  email: string,
  subjectKeyword: string,
  timeoutMs = 30_000,
): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  let lastError = "";

  while (Date.now() < deadline) {
    try {
      const messages = await getMailMessages();
      const candidates = messages
        .filter((msg) => (msg.To ?? []).some((to) => to.Address === email))
        .filter((msg) => (msg.Subject ?? "").includes(subjectKeyword))
        .sort((a, b) => (b.Created ?? "").localeCompare(a.Created ?? ""));

      if (candidates.length > 0) {
        return await getMailMessageDetail(candidates[0].ID);
      }
    } catch (error) {
      lastError = String(error);
    }

    await sleep(500);
  }

  throw new Error(
    `메일 수신 대기 타임아웃: email=${email} subject=${subjectKeyword} ${lastError}`,
  );
}

function extractVerificationCode(raw: string): string {
  const match = raw.match(/\b(\d{6})\b/);
  if (!match) {
    throw new Error("인증코드(6자리)를 메일 본문에서 찾을 수 없습니다");
  }
  return match[1];
}

function extractInvitationToken(raw: string): string {
  const urlMatch = raw.match(/token=([A-Za-z0-9._-]+)/);
  if (!urlMatch) {
    throw new Error("초대 토큰을 메일 본문에서 찾을 수 없습니다");
  }
  return urlMatch[1];
}

async function registerUserWithVerification(
  request: APIRequestContext,
  input: {
    email: string;
    fullName: string;
    orgName: string;
    slug: string;
  },
): Promise<TestAccount> {
  const sendResult = await callApi(
    request,
    "POST",
    "/api/v1/auth/send-verification",
    {
      data: {
        email: input.email,
      },
    },
  );
  expect(sendResult.status, `send-verification 실패: ${sendResult.text}`).toBe(
    200,
  );

  const mailBody = await pollMailBody(input.email, "이메일 인증코드");
  const code = extractVerificationCode(mailBody);

  const verifyResult = await callApi(
    request,
    "POST",
    "/api/v1/auth/verify-email",
    {
      data: {
        email: input.email,
        code,
      },
    },
  );
  expect(verifyResult.status, `verify-email 실패: ${verifyResult.text}`).toBe(
    200,
  );

  const verifyJson = verifyResult.json as Record<string, unknown>;
  const verificationToken = String(verifyJson.verification_token ?? "");
  expect(verificationToken).not.toBe("");

  const registerResult = await callApi(
    request,
    "POST",
    "/api/v1/auth/register",
    {
      data: {
        verification_token: verificationToken,
        code,
        password: PASSWORD,
        full_name: input.fullName,
        org_name: input.orgName,
        slug: input.slug,
        plan_type: "STARTER",
      },
    },
  );
  expect(registerResult.status, `register 실패: ${registerResult.text}`).toBe(
    200,
  );

  const registerJson = registerResult.json as Record<string, unknown>;
  const user = registerJson.user as Record<string, unknown>;
  const org = registerJson.organization as Record<string, unknown>;
  const tokens = registerJson.tokens as Record<string, unknown>;

  return {
    email: input.email,
    password: PASSWORD,
    slug: String(org.slug ?? input.slug),
    accessToken: String(tokens.access_token ?? ""),
    refreshToken: String(tokens.refresh_token ?? ""),
    userId: String(user.id ?? ""),
    orgId: String(org.id ?? ""),
  };
}

export async function registerIsolatedAccount(
  request: APIRequestContext,
  suffix: string,
): Promise<TestAccount> {
  const account = await registerUserWithVerification(request, {
    email: `pw_isolated_${suffix}@example.com`,
    fullName: "Playwright Isolated",
    orgName: `Playwright Isolated Org ${suffix}`,
    slug: `pw-isolated-${suffix}`,
  });
  return account;
}

async function createAndCompleteUpload(
  request: APIRequestContext,
  token: string,
): Promise<{ fileId: string }> {
  const csvBuffer = fs.readFileSync(CSV_PATH);

  const createResult = await callApi(request, "POST", "/api/v1/files/upload", {
    token,
    data: {
      original_name: "scope_stress_bom.csv",
      content_type: "text/csv",
      file_size: csvBuffer.byteLength,
    },
  });
  expect(
    createResult.status,
    `업로드 URL 발급 실패: ${createResult.text}`,
  ).toBe(200);

  const createJson = createResult.json as Record<string, unknown>;
  const fileId = String(createJson.file_id ?? "");
  const uploadUrl = String(createJson.upload_url ?? "");
  expect(fileId).not.toBe("");
  expect(uploadUrl).not.toBe("");

  const uploadResponse = await fetch(uploadUrl, {
    method: "PUT",
    body: csvBuffer,
    headers: {
      "Content-Type": "text/csv",
      "Content-Length": String(csvBuffer.byteLength),
    },
  });
  expect(uploadResponse.status, "presigned PUT 실패").toBe(200);

  const completeResult = await callApi(
    request,
    "POST",
    `/api/v1/files/upload/${fileId}/complete`,
    {
      token,
    },
  );
  expect(
    completeResult.status,
    `업로드 완료 실패: ${completeResult.text}`,
  ).toBe(200);

  return { fileId };
}

async function createAndCompleteImageUpload(
  request: APIRequestContext,
  token: string,
): Promise<{ fileId: string }> {
  const imageBuffer = Buffer.from(TINY_PNG_BASE64, "base64");

  const createResult = await callApi(request, "POST", "/api/v1/files/upload", {
    token,
    data: {
      original_name: "pw_profile.png",
      content_type: "image/png",
      file_size: imageBuffer.byteLength,
    },
  });
  expect(
    createResult.status,
    `이미지 업로드 URL 발급 실패: ${createResult.text}`,
  ).toBe(200);

  const createJson = createResult.json as Record<string, unknown>;
  const fileId = String(createJson.file_id ?? "");
  const uploadUrl = String(createJson.upload_url ?? "");
  expect(fileId).not.toBe("");
  expect(uploadUrl).not.toBe("");

  const uploadResponse = await fetch(uploadUrl, {
    method: "PUT",
    body: imageBuffer,
    headers: {
      "Content-Type": "image/png",
      "Content-Length": String(imageBuffer.byteLength),
    },
  });
  expect(uploadResponse.status, "이미지 presigned PUT 실패").toBe(200);

  const completeResult = await callApi(
    request,
    "POST",
    `/api/v1/files/upload/${fileId}/complete`,
    {
      token,
    },
  );
  expect(
    completeResult.status,
    `이미지 업로드 완료 실패: ${completeResult.text}`,
  ).toBe(200);

  return { fileId };
}

async function confirmMapping(
  request: APIRequestContext,
  token: string,
  fileId: string,
  suffix: string,
): Promise<string> {
  const mapping = parseJsonSafe<Record<string, unknown>>(
    fs.readFileSync(MAPPING_PATH, "utf-8"),
  );
  if (!mapping) {
    throw new Error("매핑 fixture 파싱에 실패했습니다");
  }

  const confirmResult = await callApi(
    request,
    "POST",
    "/api/v1/mappings/confirm",
    {
      token,
      data: {
        file_id: fileId,
        name: `pw-scope-mapping-${suffix}`,
        mapping,
      },
    },
  );
  expect(confirmResult.status, `매핑 확정 실패: ${confirmResult.text}`).toBe(
    200,
  );

  const confirmJson = confirmResult.json as Record<string, unknown>;
  return String(confirmJson.id ?? "");
}

async function waitForSynthesisComplete(
  request: APIRequestContext,
  token: string,
  jobId: string,
): Promise<void> {
  const deadline = Date.now() + 120_000;

  while (Date.now() < deadline) {
    const jobResult = await callApi(
      request,
      "GET",
      `/api/v1/synthesis/${jobId}`,
      {
        token,
      },
    );
    expect(jobResult.status, `합성 상태 조회 실패: ${jobResult.text}`).toBe(
      200,
    );

    const jobJson = jobResult.json as Record<string, unknown>;
    const status = String(jobJson.status ?? "");

    if (status === "COMPLETED" || status === "COMPLETED_WITH_ERRORS") {
      return;
    }

    if (status === "FAILED") {
      throw new Error(`합성 실패: ${jobResult.text}`);
    }

    await sleep(1_000);
  }

  throw new Error(`합성 완료 대기 타임아웃: job_id=${jobId}`);
}

export async function bootstrapSession(
  request: APIRequestContext,
): Promise<TestSession> {
  if (cachedSessionPromise) {
    return cachedSessionPromise;
  }

  cachedSessionPromise = (async () => {
    const suffix = createSuffix();

    const owner = await registerUserWithVerification(request, {
      email: `pw_owner_${suffix}@example.com`,
      fullName: "Playwright Owner",
      orgName: `Playwright Org ${suffix}`,
      slug: `pw-${suffix}`,
    });

    const meResult = await callApi(request, "GET", "/api/v1/users/me", {
      token: owner.accessToken,
    });
    expect(meResult.status, `users/me 실패: ${meResult.text}`).toBe(200);

    const meJson = meResult.json as Record<string, unknown>;
    const meUser = meJson.user as Record<string, unknown>;
    owner.userId = String(meUser.id ?? owner.userId);

    const upload = await createAndCompleteUpload(request, owner.accessToken);
    const imageUpload = await createAndCompleteImageUpload(
      request,
      owner.accessToken,
    );
    const mappingId = await confirmMapping(
      request,
      owner.accessToken,
      upload.fileId,
      suffix,
    );

    const synthesisStart = await callApi(request, "POST", "/api/v1/synthesis", {
      token: owner.accessToken,
      data: {
        mapping_id: mappingId,
        uploads: [{ file_id: upload.fileId }],
        overwrite: false,
      },
    });
    expect(
      synthesisStart.status,
      `합성 시작 실패: ${synthesisStart.text}`,
    ).toBe(200);

    const synthJson = synthesisStart.json as Record<string, unknown>;
    const batchId = String(synthJson.batch_id ?? "");
    const items = (synthJson.items as Array<Record<string, unknown>>) ?? [];
    const jobId = String(items[0]?.id ?? "");
    expect(jobId).not.toBe("");

    await waitForSynthesisComplete(request, owner.accessToken, jobId);

    const partsResult = await callApi(request, "GET", "/api/v1/parts", {
      token: owner.accessToken,
      params: {
        limit: 100,
      },
    });
    expect(partsResult.status, `parts 조회 실패: ${partsResult.text}`).toBe(
      200,
    );

    const partsJson = partsResult.json as Record<string, unknown>;
    const partItems = (partsJson.items as Array<Record<string, unknown>>) ?? [];
    const firstPart = partItems[0] ?? {};
    const rootPart =
      partItems.find((item) =>
        String(item.part_number ?? "").includes("WIDE-ROOT"),
      ) ?? firstPart;

    const partId = String(firstPart.id ?? "");
    const asmPartId = String(rootPart.id ?? partId);
    const category = String(firstPart.category ?? "미분류");

    const projectResult = await callApi(request, "POST", "/api/v1/projects", {
      token: owner.accessToken,
      data: {
        name: `PW Project ${suffix}`,
        description: "Playwright 통합 테스트 프로젝트",
      },
    });
    expect(
      projectResult.status,
      `프로젝트 생성 실패: ${projectResult.text}`,
    ).toBe(201);
    const projectId = String(
      (projectResult.json as Record<string, unknown>).id ?? "",
    );

    const teamResult = await callApi(request, "POST", "/api/v1/teams", {
      token: owner.accessToken,
      data: {
        name: `PW Team ${suffix}`,
        description: "Playwright 테스트 팀",
      },
    });
    expect(teamResult.status, `팀 생성 실패: ${teamResult.text}`).toBe(201);
    const teamId = String(
      (teamResult.json as Record<string, unknown>).id ?? "",
    );

    const labelResult = await callApi(request, "POST", "/api/v1/labels", {
      token: owner.accessToken,
      data: {
        name: `pw-label-${suffix}`,
        color: "#1E88E5",
        description: "Playwright 테스트 라벨",
      },
    });
    expect(labelResult.status, `라벨 생성 실패: ${labelResult.text}`).toBe(201);
    const labelId = String(
      (labelResult.json as Record<string, unknown>).id ?? "",
    );

    const issueResult = await callApi(request, "POST", "/api/v1/issues", {
      token: owner.accessToken,
      data: {
        title: `PW Issue ${suffix}`,
        part_ids: partId ? [partId] : [],
        label_ids: labelId ? [labelId] : [],
      },
    });
    expect(issueResult.status, `이슈 생성 실패: ${issueResult.text}`).toBe(201);

    const issueJson = issueResult.json as Record<string, unknown>;
    const issueNumber = Number(issueJson.number ?? 0);
    const issueId = String(issueJson.id ?? "");

    const issueCommentResult = await callApi(
      request,
      "POST",
      `/api/v1/issues/${issueNumber}/comments`,
      {
        token: owner.accessToken,
        data: {
          body: {
            type: "doc",
            content: [],
          },
        },
      },
    );
    expect(
      issueCommentResult.status,
      `이슈 댓글 생성 실패: ${issueCommentResult.text}`,
    ).toBe(201);
    const issueCommentId = String(
      (issueCommentResult.json as Record<string, unknown>).id ?? "",
    );

    const changeResult = await callApi(request, "POST", "/api/v1/changes", {
      token: owner.accessToken,
      data: {
        title: `PW Change ${suffix}`,
        issue_number: issueNumber,
      },
    });
    expect(
      changeResult.status,
      `변경요청 생성 실패: ${changeResult.text}`,
    ).toBe(201);

    const changeJson = changeResult.json as Record<string, unknown>;
    const changeNumber = Number(changeJson.number ?? 0);
    const changeId = String(changeJson.id ?? "");

    const changeCommentResult = await callApi(
      request,
      "POST",
      `/api/v1/changes/${changeNumber}/comments`,
      {
        token: owner.accessToken,
        data: {
          body: {
            type: "doc",
            content: [],
          },
        },
      },
    );
    expect(
      changeCommentResult.status,
      `변경요청 댓글 생성 실패: ${changeCommentResult.text}`,
    ).toBe(201);
    const changeCommentId = String(
      (changeCommentResult.json as Record<string, unknown>).id ?? "",
    );

    const invitedEmail = `pw_member_${suffix}@example.com`;
    const invitationResult = await callApi(
      request,
      "POST",
      "/api/v1/organizations/invitations",
      {
        token: owner.accessToken,
        data: {
          email: invitedEmail,
          role: "MEMBER",
        },
      },
    );
    expect(
      invitationResult.status,
      `초대 생성 실패: ${invitationResult.text}`,
    ).toBe(201);
    const invitationId = String(
      (invitationResult.json as Record<string, unknown>).id ?? "",
    );

    const invitationMailBody = await pollMailBody(
      invitedEmail,
      "워크스페이스에 초대되었습니다",
    );
    const invitationToken = extractInvitationToken(invitationMailBody);

    const acceptInvitationResult = await callApi(
      request,
      "POST",
      "/api/v1/auth/accept-invitation",
      {
        data: {
          token: invitationToken,
          password: PASSWORD,
          full_name: "Playwright Member",
        },
      },
    );
    expect(
      acceptInvitationResult.status,
      `초대 수락 실패: ${acceptInvitationResult.text}`,
    ).toBe(200);

    const acceptJson = acceptInvitationResult.json as Record<string, unknown>;
    const memberUser = acceptJson.user as Record<string, unknown>;
    const memberOrg = acceptJson.organization as Record<string, unknown>;
    const memberTokens = acceptJson.tokens as Record<string, unknown>;

    const member: TestAccount = {
      email: invitedEmail,
      password: PASSWORD,
      slug: String(memberOrg.slug ?? owner.slug),
      accessToken: String(memberTokens.access_token ?? ""),
      refreshToken: String(memberTokens.refresh_token ?? ""),
      userId: String(memberUser.id ?? ""),
      orgId: String(memberOrg.id ?? ""),
    };

    const addProjectMemberResult = await callApi(
      request,
      "POST",
      `/api/v1/projects/${projectId}/members`,
      {
        token: owner.accessToken,
        data: {
          user_ids: [member.userId],
          role: "MEMBER",
        },
      },
    );
    expect(addProjectMemberResult.status).toBe(201);

    const addTeamMemberResult = await callApi(
      request,
      "POST",
      `/api/v1/teams/${teamId}/members`,
      {
        token: owner.accessToken,
        data: {
          user_ids: [member.userId],
        },
      },
    );
    expect(addTeamMemberResult.status).toBe(201);

    const notificationsResult = await callApi(
      request,
      "GET",
      "/api/v1/notifications",
      {
        token: owner.accessToken,
        params: {
          limit: 1,
        },
      },
    );
    expect(notificationsResult.status).toBe(200);

    const notificationsJson = notificationsResult.json as Record<
      string,
      unknown
    >;
    const notificationItems =
      (notificationsJson.items as Array<Record<string, unknown>>) ?? [];
    const notificationId = String(notificationItems[0]?.id ?? randomUUID());

    return {
      suffix,
      owner,
      member,
      resources: {
        fileId: upload.fileId,
        imageFileId: imageUpload.fileId,
        mappingId,
        synthesisJobId: jobId,
        synthesisBatchId: batchId,
        projectId,
        teamId,
        labelId,
        issueNumber,
        issueId,
        changeNumber,
        changeId,
        issueCommentId,
        changeCommentId,
        partId,
        asmPartId,
        category,
        invitationId,
        invitationToken,
        notificationId,
      },
    };
  })();

  return cachedSessionPromise;
}
