"""테넌트 DB 의존성 회귀 테스트."""

import types
import unittest
import uuid
from unittest.mock import patch

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.core.exceptions import AppError


class _FakeSession:
    def __init__(self) -> None:
        self.executed: list[str] = []
        self.closed = False

    def execute(self, statement) -> None:
        self.executed.append(str(statement))

    def close(self) -> None:
        self.closed = True


class TenantDependencyTests(unittest.TestCase):
    def test_require_auth_raises_when_context_missing(self) -> None:
        request = types.SimpleNamespace(state=types.SimpleNamespace())

        with self.assertRaises(AppError) as ctx:
            require_auth(request)

        self.assertEqual(ctx.exception.code, "UNAUTHENTICATED")

    def test_get_tenant_db_sets_search_path_from_auth_org_id(self) -> None:
        fake_session = _FakeSession()
        auth = AuthContext(
            account_id=uuid.uuid4(),
            email="user@example.com",
            org_id=uuid.uuid4(),
        )

        with (
            patch("app.api.deps.SessionLocal", return_value=fake_session),
            patch("app.api.deps.org_id_to_schema", return_value="tenant_testorg"),
        ):
            dep = get_tenant_db(auth=auth)
            yielded = next(dep)

            self.assertIs(yielded, fake_session)
            self.assertEqual(len(fake_session.executed), 1)
            self.assertEqual(
                fake_session.executed[0],
                "SET search_path = tenant_testorg, ag_catalog, public",
            )

            dep.close()
            self.assertTrue(fake_session.closed)

    def test_get_tenant_db_uses_different_schema_per_org(self) -> None:
        session_a = _FakeSession()
        session_b = _FakeSession()
        auth_a = AuthContext(
            account_id=uuid.uuid4(),
            email="a@example.com",
            org_id=uuid.uuid4(),
        )
        auth_b = AuthContext(
            account_id=uuid.uuid4(),
            email="b@example.com",
            org_id=uuid.uuid4(),
        )

        with patch("app.api.deps.SessionLocal", side_effect=[session_a, session_b]):
            with patch(
                "app.api.deps.org_id_to_schema",
                side_effect=["tenant_org_a", "tenant_org_b"],
            ):
                dep_a = get_tenant_db(auth=auth_a)
                next(dep_a)
                dep_b = get_tenant_db(auth=auth_b)
                next(dep_b)

                self.assertEqual(
                    session_a.executed[0],
                    "SET search_path = tenant_org_a, ag_catalog, public",
                )
                self.assertEqual(
                    session_b.executed[0],
                    "SET search_path = tenant_org_b, ag_catalog, public",
                )

                dep_a.close()
                dep_b.close()

                self.assertTrue(session_a.closed)
                self.assertTrue(session_b.closed)


if __name__ == "__main__":
    unittest.main()
