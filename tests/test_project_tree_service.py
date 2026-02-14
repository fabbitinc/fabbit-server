"""프로젝트 트리 서비스 테스트."""

import types
import unittest
import uuid
from datetime import UTC, datetime
from typing import cast
from unittest.mock import patch

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.modules.project import service


def _dt() -> datetime:
    return datetime(2026, 2, 14, tzinfo=UTC)


def _project(
    *,
    project_id: uuid.UUID,
    name: str,
    description: str | None = None,
) -> types.SimpleNamespace:
    return types.SimpleNamespace(
        id=project_id,
        name=name,
        description=description,
        created_at=_dt(),
        updated_at=_dt(),
    )


def _folder(
    *,
    folder_id: uuid.UUID,
    name: str,
    project_id: uuid.UUID | None,
    parent_id: uuid.UUID | None = None,
) -> types.SimpleNamespace:
    return types.SimpleNamespace(
        id=folder_id,
        name=name,
        project_id=project_id,
        parent_id=parent_id,
        created_at=_dt(),
    )


class ProjectTreeServiceTests(unittest.TestCase):
    def _auth(self) -> AuthContext:
        return AuthContext(
            account_id=uuid.uuid4(),
            email="tester@example.com",
            org_id=uuid.uuid4(),
        )

    def test_get_projects_tree_builds_nested_folders_and_stats(self) -> None:
        project_id = uuid.uuid4()
        root_folder_id = uuid.uuid4()
        child_folder_id = uuid.uuid4()

        projects = [_project(project_id=project_id, name="Alpha")]
        folders = [
            _folder(folder_id=root_folder_id, name="A", project_id=project_id),
            _folder(
                folder_id=child_folder_id,
                name="B",
                project_id=project_id,
                parent_id=root_folder_id,
            ),
        ]

        with (
            patch(
                "app.modules.project.service.repo.list_projects", return_value=projects
            ),
            patch(
                "app.modules.project.service.repo.list_folders", return_value=folders
            ),
            patch(
                "app.modules.project.service.repo.get_project_upload_counts",
                return_value={project_id: 2},
            ),
            patch(
                "app.modules.project.service.repo.get_project_drawing_counts",
                return_value={project_id: 3},
            ),
            patch(
                "app.modules.project.service.repo.get_project_folder_counts",
                return_value={project_id: 2},
            ),
            patch(
                "app.modules.project.service.repo.get_folder_drawing_counts",
                return_value={root_folder_id: 1, child_folder_id: 4},
            ),
        ):
            result = service.get_projects_tree(
                db=cast(Session, object()),
                _auth=self._auth(),
            )

        self.assertEqual(result.meta.project_count, 1)
        self.assertEqual(result.meta.folder_count, 2)
        self.assertEqual(len(result.projects), 1)
        self.assertEqual(len(result.orphans), 0)

        project = result.projects[0]
        self.assertEqual(project.stats.upload_count, 2)
        self.assertEqual(project.stats.drawing_count, 3)
        self.assertEqual(project.stats.folder_count, 2)
        self.assertEqual(len(project.folders), 1)

        root = project.folders[0]
        self.assertEqual(root.id, root_folder_id)
        self.assertEqual(root.stats.drawing_count, 1)
        self.assertEqual(len(root.folders), 1)
        self.assertEqual(root.folders[0].id, child_folder_id)
        self.assertEqual(root.folders[0].stats.drawing_count, 4)

        self.assertEqual(root.items, [])
        self.assertEqual(root.item_count, 0)

    def test_get_projects_tree_puts_unknown_or_unassigned_into_orphans(self) -> None:
        project_id = uuid.uuid4()
        unknown_project_id = uuid.uuid4()

        projects = [_project(project_id=project_id, name="Alpha")]
        folders = [
            _folder(folder_id=uuid.uuid4(), name="No Project", project_id=None),
            _folder(
                folder_id=uuid.uuid4(),
                name="Unknown Project",
                project_id=unknown_project_id,
            ),
        ]

        with (
            patch(
                "app.modules.project.service.repo.list_projects", return_value=projects
            ),
            patch(
                "app.modules.project.service.repo.list_folders", return_value=folders
            ),
            patch(
                "app.modules.project.service.repo.get_project_upload_counts",
                return_value={},
            ),
            patch(
                "app.modules.project.service.repo.get_project_drawing_counts",
                return_value={},
            ),
            patch(
                "app.modules.project.service.repo.get_project_folder_counts",
                return_value={},
            ),
            patch(
                "app.modules.project.service.repo.get_folder_drawing_counts",
                return_value={},
            ),
        ):
            result = service.get_projects_tree(
                db=cast(Session, object()),
                _auth=self._auth(),
            )

        self.assertEqual(len(result.projects[0].folders), 0)
        self.assertEqual(len(result.orphans), 2)

    def test_get_projects_tree_avoids_cycle_and_cross_project_attach(self) -> None:
        project_a = uuid.uuid4()
        project_b = uuid.uuid4()
        folder_a = uuid.uuid4()
        folder_b = uuid.uuid4()
        folder_beta_root = uuid.uuid4()
        folder_cross_child = uuid.uuid4()

        projects = [
            _project(project_id=project_a, name="Alpha"),
            _project(project_id=project_b, name="Beta"),
        ]
        folders = [
            _folder(
                folder_id=folder_a,
                name="Cycle A",
                project_id=project_a,
                parent_id=folder_b,
            ),
            _folder(
                folder_id=folder_b,
                name="Cycle B",
                project_id=project_a,
                parent_id=folder_a,
            ),
            _folder(
                folder_id=folder_beta_root,
                name="Beta Root",
                project_id=project_b,
            ),
            _folder(
                folder_id=folder_cross_child,
                name="Cross Child",
                project_id=project_a,
                parent_id=folder_beta_root,
            ),
        ]

        with (
            patch(
                "app.modules.project.service.repo.list_projects", return_value=projects
            ),
            patch(
                "app.modules.project.service.repo.list_folders", return_value=folders
            ),
            patch(
                "app.modules.project.service.repo.get_project_upload_counts",
                return_value={},
            ),
            patch(
                "app.modules.project.service.repo.get_project_drawing_counts",
                return_value={},
            ),
            patch(
                "app.modules.project.service.repo.get_project_folder_counts",
                return_value={project_a: 3},
            ),
            patch(
                "app.modules.project.service.repo.get_folder_drawing_counts",
                return_value={},
            ),
        ):
            result = service.get_projects_tree(
                db=cast(Session, object()),
                _auth=self._auth(),
            )

        alpha = result.projects[0]
        self.assertEqual(alpha.name, "Alpha")
        # 순환 관계(A<->B)는 attach되지 않아 루트로 남아야 한다.
        self.assertEqual(len(alpha.folders), 3)
        folder_names = {f.name for f in alpha.folders}
        self.assertTrue({"Cycle A", "Cycle B", "Cross Child"}.issubset(folder_names))


if __name__ == "__main__":
    unittest.main()
