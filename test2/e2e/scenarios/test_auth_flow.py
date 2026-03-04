"""인증/계정 e2e 시나리오."""

import uuid

import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e]


class TestAuthFlow:
    """회원가입/로그인/토큰/인증 예외 시나리오."""

    access_token: str = ""
    refresh_token: str = ""
    slug: str = ""

    def test_health(self, client: TestClient):
        resp = client.get("/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"

    def test_plans(self, client: TestClient):
        resp = client.get("/api/v1/auth/plans")
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) >= 1
        assert data[0]["plan_type"]

    def test_register(self, client: TestClient, unique_suffix: str):
        TestAuthFlow.slug = f"auth-test-{unique_suffix}"
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"auth_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "Auth 테스트",
                "org_name": f"AuthOrg_{unique_suffix}",
                "slug": TestAuthFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestAuthFlow.access_token = data["tokens"]["access_token"]
        TestAuthFlow.refresh_token = data["tokens"]["refresh_token"]

    def test_check_email_taken(self, client: TestClient, unique_suffix: str):
        resp = client.get(
            "/api/v1/auth/check-email",
            params={"email": f"auth_{unique_suffix}@test.com"},
        )
        assert resp.status_code == 200
        assert resp.json()["available"] is False

    def test_check_slug_taken(self, client: TestClient):
        resp = client.get(
            "/api/v1/auth/check-slug",
            params={"slug": TestAuthFlow.slug},
        )
        assert resp.status_code == 200
        assert resp.json()["available"] is False

    def test_site(self, client: TestClient):
        resp = client.get(
            "/api/v1/auth/site",
            headers={"Origin": f"http://{TestAuthFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 200
        assert resp.json()["slug"] == TestAuthFlow.slug

    def test_login(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/login",
            json={
                "email": f"auth_{unique_suffix}@test.com",
                "password": "TestPass1234",
            },
            headers={"Origin": f"http://{TestAuthFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestAuthFlow.access_token = data["tokens"]["access_token"]
        TestAuthFlow.refresh_token = data["tokens"]["refresh_token"]

    def test_me(self, client: TestClient):
        resp = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {TestAuthFlow.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["memberships"][0]["organization"]["slug"] == TestAuthFlow.slug

    def test_token_refresh(self, client: TestClient):
        resp = client.post(
            "/api/v1/auth/refresh",
            json={"refresh_token": TestAuthFlow.refresh_token},
        )
        assert resp.status_code == 200
        data = resp.json()
        TestAuthFlow.access_token = data["access_token"]
        TestAuthFlow.refresh_token = data["refresh_token"]

    def test_login_wrong_password(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/login",
            json={
                "email": f"auth_{unique_suffix}@test.com",
                "password": "WrongPassword",
            },
            headers={"Origin": f"http://{TestAuthFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 401

    def test_register_duplicate_email(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"auth_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "중복 테스트",
                "org_name": "DupOrg",
                "slug": f"dup-auth-email-{unique_suffix}",
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code in (400, 409)

    def test_register_duplicate_slug(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"newauth_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "중복 테스트",
                "org_name": "DupSlugOrg",
                "slug": TestAuthFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code in (400, 409)

    def test_logout(self, client: TestClient):
        resp = client.post(
            "/api/v1/auth/logout",
            headers={"Authorization": f"Bearer {TestAuthFlow.access_token}"},
            json={"refresh_token": TestAuthFlow.refresh_token},
        )
        assert resp.status_code == 204

    def test_parts_without_auth(self, client: TestClient):
        resp = client.get("/api/v1/parts")
        assert resp.status_code == 401

    def test_get_part_not_found(self, client: TestClient):
        resp = client.get(
            f"/api/v1/parts/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestAuthFlow.access_token}"},
        )
        assert resp.status_code == 404
