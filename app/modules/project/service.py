"""프로젝트 도메인 비즈니스 로직."""

import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.part import repository as part_repo
from app.modules.project import repository as repo
from app.modules.project.models import Folder
from app.modules.project.schemas import (
    CreateFolderRequest,
    CreateProjectRequest,
    FolderResponse,
    FolderStats,
    FolderTreeNode,
    MoveFolderRequest,
    ProjectPartListResponse,
    ProjectPartResponse,
    ProjectResponse,
    ProjectStats,
    ProjectTreeMeta,
    ProjectTreeNode,
    ProjectTreeResponse,
    UpdateFolderRequest,
    UpdateProjectRequest,
)
from app.modules.file import repository as file_repo


# ── 트리 조회 (기존) ──


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


@transactional(read_only=True)
def get_projects_tree(db: Session, _auth: AuthContext) -> ProjectTreeResponse:
    projects = repo.list_projects(db)
    folders = repo.list_folders(db)

    project_file_counts = repo.get_project_file_counts(db)
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
                upload_count=project_file_counts.get(project.id, 0),
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


# ── Project CRUD ──


@transactional
def create_project(
    db: Session,
    auth: AuthContext,
    req: CreateProjectRequest,
) -> ProjectResponse:
    project = repo.create_project(db, req.name, req.description)
    logger.info("프로젝트 생성: id={id} name={name}", id=project.id, name=project.name)
    return _to_project_response(project)


@transactional(read_only=True)
def get_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> ProjectResponse:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")
    return _to_project_response(project)


@transactional
def update_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    req: UpdateProjectRequest,
) -> ProjectResponse:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")
    repo.update_project(db, project, req.name, req.description)
    logger.info("프로젝트 수정: id={id}", id=project.id)
    return _to_project_response(project)


@transactional
def delete_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> None:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    # 프로젝트 소속 폴더의 Upload cascade 삭제
    all_folder_ids = repo.get_folder_ids_by_project(db, project_id)
    for fid in all_folder_ids:
        file_repo.delete_files_by_owner(db, "folder", fid)

    # 프로젝트 소속 Upload cascade 삭제
    file_repo.delete_files_by_owner(db, "project", project_id)
    # 프로젝트 삭제 (Folder, ProjectPart는 DB CASCADE로 자동 삭제)
    repo.delete_project(db, project_id)
    logger.info("프로젝트 삭제: id={id}", id=project_id)


def _to_project_response(project) -> ProjectResponse:
    return ProjectResponse(
        id=project.id,
        name=project.name,
        description=project.description,
        created_at=project.created_at,
        updated_at=project.updated_at,
    )


# ── Folder CRUD ──


@transactional
def create_folder(
    db: Session,
    auth: AuthContext,
    req: CreateFolderRequest,
) -> FolderResponse:
    # 프로젝트 존재 검증
    project = repo.get_project_by_id(db, req.project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    # 부모 폴더 존재 및 동일 프로젝트 검증
    if req.parent_id is not None:
        parent = repo.get_folder_by_id(db, req.parent_id)
        if parent is None:
            raise AppError(message="부모 폴더를 찾을 수 없습니다", code="NOT_FOUND")
        if parent.project_id != req.project_id:
            raise AppError(
                message="부모 폴더가 다른 프로젝트에 속해 있습니다",
                code="VALIDATION_ERROR",
            )

    folder = repo.create_folder(db, req.name, req.project_id, req.parent_id)
    logger.info("폴더 생성: id={id} name={name}", id=folder.id, name=folder.name)
    return _to_folder_response(folder)


@transactional
def update_folder(
    db: Session,
    auth: AuthContext,
    folder_id: uuid.UUID,
    req: UpdateFolderRequest,
) -> FolderResponse:
    folder = repo.get_folder_by_id(db, folder_id)
    if folder is None:
        raise AppError(message="폴더를 찾을 수 없습니다", code="NOT_FOUND")
    repo.update_folder(db, folder, req.name)
    logger.info("폴더 수정: id={id}", id=folder.id)
    return _to_folder_response(folder)


@transactional
def move_folder(
    db: Session,
    auth: AuthContext,
    folder_id: uuid.UUID,
    req: MoveFolderRequest,
) -> FolderResponse:
    folder = repo.get_folder_by_id(db, folder_id)
    if folder is None:
        raise AppError(message="폴더를 찾을 수 없습니다", code="NOT_FOUND")

    # 순환 참조 검증
    if req.parent_id is not None:
        if req.parent_id == folder_id:
            raise AppError(
                message="폴더를 자기 자신의 하위로 이동할 수 없습니다",
                code="VALIDATION_ERROR",
            )
        # 대상 부모가 현재 폴더의 하위인지 검사
        descendants = repo.get_descendant_folder_ids(db, folder_id)
        if req.parent_id in descendants:
            raise AppError(
                message="하위 폴더로 이동할 수 없습니다 (순환 참조)",
                code="VALIDATION_ERROR",
            )
        # 대상 부모 존재 및 동일 프로젝트 검증
        parent = repo.get_folder_by_id(db, req.parent_id)
        if parent is None:
            raise AppError(message="대상 부모 폴더를 찾을 수 없습니다", code="NOT_FOUND")
        if parent.project_id != folder.project_id:
            raise AppError(
                message="다른 프로젝트의 폴더로 이동할 수 없습니다",
                code="VALIDATION_ERROR",
            )

    repo.move_folder(db, folder, req.parent_id)
    logger.info("폴더 이동: id={id} parent_id={parent_id}", id=folder.id, parent_id=req.parent_id)
    return _to_folder_response(folder)


@transactional
def delete_folder(
    db: Session,
    auth: AuthContext,
    folder_id: uuid.UUID,
) -> None:
    folder = repo.get_folder_by_id(db, folder_id)
    if folder is None:
        raise AppError(message="폴더를 찾을 수 없습니다", code="NOT_FOUND")

    # 하위 폴더의 Upload cascade 삭제
    descendant_ids = repo.get_descendant_folder_ids(db, folder_id)
    for fid in descendant_ids:
        file_repo.delete_files_by_owner(db, "folder", fid)

    # 현재 폴더의 Upload 삭제
    file_repo.delete_files_by_owner(db, "folder", folder_id)
    # 폴더 삭제 (하위 폴더는 DB CASCADE)
    repo.delete_folder(db, folder_id)
    logger.info("폴더 삭제: id={id}", id=folder_id)


def _to_folder_response(folder) -> FolderResponse:
    return FolderResponse(
        id=folder.id,
        name=folder.name,
        parent_id=folder.parent_id,
        project_id=folder.project_id,
        created_at=folder.created_at,
    )


# ── ProjectPart (프로젝트-파트 연결, RDS + Graph dual-write) ──


@transactional(read_only=True)
def get_project_parts(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> ProjectPartListResponse:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    parts = repo.get_project_parts(db, project_id)
    return ProjectPartListResponse(
        parts=[
            ProjectPartResponse(
                id=p.id,
                part_number=p.part_number,
                name=p.name,
                category=p.category,
            )
            for p in parts
        ],
    )


@transactional
def add_part_to_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    part_id: uuid.UUID,
) -> None:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    part = part_repo.get_by_id(db, part_id)
    if part is None:
        raise AppError(message="부품을 찾을 수 없습니다", code="NOT_FOUND")

    graph_name = org_id_to_schema(auth.org_id)
    repo.add_part_to_project(
        db, project_id, part_id,
        project_name=project.name,
        part_number=part.part_number,
        graph_name=graph_name,
    )
    logger.info(
        "프로젝트-파트 연결: project_id={project_id} part_id={part_id}",
        project_id=project_id,
        part_id=part_id,
    )


@transactional
def remove_part_from_project(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    part_id: uuid.UUID,
) -> None:
    project = repo.get_project_by_id(db, project_id)
    if project is None:
        raise AppError(message="프로젝트를 찾을 수 없습니다", code="NOT_FOUND")

    part = part_repo.get_by_id(db, part_id)
    if part is None:
        raise AppError(message="부품을 찾을 수 없습니다", code="NOT_FOUND")

    graph_name = org_id_to_schema(auth.org_id)
    repo.remove_part_from_project(
        db, project_id, part_id,
        project_name=project.name,
        part_number=part.part_number,
        graph_name=graph_name,
    )
    logger.info(
        "프로젝트-파트 연결 해제: project_id={project_id} part_id={part_id}",
        project_id=project_id,
        part_id=part_id,
    )
