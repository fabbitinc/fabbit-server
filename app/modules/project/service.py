"""프로젝트 트리 조회 비즈니스 로직."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.modules.project import repository as repo
from app.modules.project.models import Folder
from app.modules.project.schemas import (
    FolderStats,
    FolderTreeNode,
    ProjectStats,
    ProjectTreeMeta,
    ProjectTreeNode,
    ProjectTreeResponse,
)


def _to_folder_node(folder: Folder, drawing_count: int) -> FolderTreeNode:
    return FolderTreeNode(
        id=folder.id,
        name=folder.name,
        parent_id=folder.parent_id,
        project_id=folder.project_id,
        created_at=folder.created_at,
        stats=FolderStats(drawing_count=drawing_count),
    )


def _would_create_cycle(
    *,
    child_id: uuid.UUID,
    parent_id: uuid.UUID | None,
    parent_by_id: dict[uuid.UUID, uuid.UUID | None],
) -> bool:
    cursor = parent_id
    visited: set[uuid.UUID] = set()

    while cursor is not None:
        if cursor == child_id:
            return True
        if cursor in visited:
            return True
        visited.add(cursor)
        cursor = parent_by_id.get(cursor)

    return False


def _sort_folder_tree(nodes: list[FolderTreeNode]) -> None:
    nodes.sort(key=lambda n: (n.name.lower(), str(n.id)))
    for node in nodes:
        _sort_folder_tree(node.folders)


def get_projects_tree(db: Session, _auth: AuthContext) -> ProjectTreeResponse:
    projects = repo.list_projects(db)
    folders = repo.list_folders(db)

    project_upload_counts = repo.get_project_upload_counts(db)
    project_drawing_counts = repo.get_project_drawing_counts(db)
    project_folder_counts = repo.get_project_folder_counts(db)
    folder_drawing_counts = repo.get_folder_drawing_counts(db)

    project_nodes: list[ProjectTreeNode] = []
    project_index: dict[uuid.UUID, ProjectTreeNode] = {}

    for project in projects:
        node = ProjectTreeNode(
            id=project.id,
            name=project.name,
            description=project.description,
            created_at=project.created_at,
            updated_at=project.updated_at,
            stats=ProjectStats(
                upload_count=project_upload_counts.get(project.id, 0),
                drawing_count=project_drawing_counts.get(project.id, 0),
                folder_count=project_folder_counts.get(project.id, 0),
            ),
        )
        project_nodes.append(node)
        project_index[project.id] = node

    folder_nodes: dict[uuid.UUID, FolderTreeNode] = {}
    parent_by_id: dict[uuid.UUID, uuid.UUID | None] = {}
    attached_children: set[uuid.UUID] = set()

    for folder in folders:
        parent_by_id[folder.id] = folder.parent_id
        folder_nodes[folder.id] = _to_folder_node(
            folder,
            drawing_count=folder_drawing_counts.get(folder.id, 0),
        )

    for folder in folders:
        if folder.parent_id is None:
            continue

        parent_node = folder_nodes.get(folder.parent_id)
        child_node = folder_nodes[folder.id]
        if parent_node is None:
            continue

        if folder.project_id != parent_node.project_id:
            continue

        if _would_create_cycle(
            child_id=folder.id,
            parent_id=folder.parent_id,
            parent_by_id=parent_by_id,
        ):
            continue

        parent_node.folders.append(child_node)
        attached_children.add(folder.id)

    root_folders = [
        node
        for folder_id, node in folder_nodes.items()
        if folder_id not in attached_children
    ]

    orphans: list[FolderTreeNode] = []
    for root in root_folders:
        if root.project_id is None:
            orphans.append(root)
            continue

        project_node = project_index.get(root.project_id)
        if project_node is None:
            orphans.append(root)
            continue

        project_node.folders.append(root)

    for project_node in project_nodes:
        _sort_folder_tree(project_node.folders)

    _sort_folder_tree(orphans)

    return ProjectTreeResponse(
        projects=project_nodes,
        orphans=orphans,
        meta=ProjectTreeMeta(
            project_count=len(project_nodes), folder_count=len(folders)
        ),
    )
