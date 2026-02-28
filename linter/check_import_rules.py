"""아키텍처 Import 규칙 검증.

규칙:
1. models.py: 타 모듈 models import는 TYPE_CHECKING 내에서만
2. repository.py: 타 모듈 repository import 금지
3. api/: repository 직접 import 금지 (service만 호출)
4. modules/: api layer import 금지 (역방향 의존)
5. use_cases/: infrastructure, repository import 금지 (service만 호출)
6. service.py: 타 모듈 service import 금지
7. service.py: 타 모듈 repository import 금지
8. queries/: service import 금지 (repo만 호출)
9. api/: service 직접 import 금지 (queries/use_cases 경유)
10. queries/: infrastructure import 금지 (repo만 호출)
11. handlers.py: service import 금지 (순환 의존 방지)
12. service.py: use_case import 금지 (역방향 의존)
13. repository.py: age_client 외 infrastructure import 금지 (URL 변환 등은 mapper로)
14. service.py: transactional 데코레이터 import 금지 (트랜잭션은 use_case가 관리)
"""

import ast
import re
from pathlib import Path

import pytest

_ROOT = Path(__file__).resolve().parent.parent
_MODULES_DIR = _ROOT / "app" / "modules"
_API_DIR = _ROOT / "app" / "api"
_USE_CASES_DIR = _ROOT / "app" / "use_cases"
_QUERIES_DIR = _ROOT / "app" / "queries"


def _get_import_module_names(node: ast.AST) -> list[str]:
    """Import/ImportFrom 노드에서 모듈 경로를 추출."""
    if isinstance(node, ast.Import):
        return [alias.name for alias in node.names]
    if isinstance(node, ast.ImportFrom) and node.module:
        return [node.module]
    return []


_PARENT_MODULE_PATTERN = re.compile(r"^app\.modules\.(\w+)$")


def _imports_submodule(node: ast.AST, submodule: str) -> str | None:
    """``from app.modules.X import {submodule}`` 패턴에서 X(모듈명)를 반환.

    해당 패턴이 아니면 None.
    """
    if isinstance(node, ast.ImportFrom) and node.module and node.names:
        m = _PARENT_MODULE_PATTERN.match(node.module)
        if m and any(alias.name == submodule for alias in node.names):
            return m.group(1)
    return None


def _is_inside_type_checking(node: ast.AST, tree: ast.Module) -> bool:
    """노드가 `if TYPE_CHECKING:` 블록 안에 있는지 확인."""
    for top_node in ast.walk(tree):
        if not isinstance(top_node, ast.If):
            continue
        # if TYPE_CHECKING: 패턴 매칭
        test = top_node.test
        is_tc = (isinstance(test, ast.Name) and test.id == "TYPE_CHECKING") or (
            isinstance(test, ast.Attribute) and test.attr == "TYPE_CHECKING"
        )
        if not is_tc:
            continue
        for child in ast.walk(top_node):
            if child is node:
                return True
    return False


def _extract_module_name(file_path: Path) -> str | None:
    """파일 경로에서 모듈 이름 추출. 예: app/modules/part/models.py → part"""
    parts = file_path.relative_to(_ROOT).parts
    # app/modules/{name}/...
    if len(parts) >= 3 and parts[0] == "app" and parts[1] == "modules":
        return parts[2]
    return None


# --- 규칙 1: models.py — 타 모듈 models import는 TYPE_CHECKING 내에서만 ---


def check_models_no_cross_module_import():
    """models.py에서 타 모듈 models를 TYPE_CHECKING 밖에서 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.(\w+)\.models")

    for models_file in _MODULES_DIR.glob("*/models.py"):
        own_module = _extract_module_name(models_file)
        source = models_file.read_text()
        tree = ast.parse(source, filename=str(models_file))

        for node in ast.walk(tree):
            target_module = None
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if m:
                    target_module = m.group(1)
            if not target_module:
                target_module = _imports_submodule(node, "models")
            if not target_module or target_module == own_module:
                continue
            if _is_inside_type_checking(node, tree):
                continue
            rel = models_file.relative_to(_ROOT)
            violations.append(
                f"  {rel}:{node.lineno} — "
                f"TYPE_CHECKING 밖에서 {target_module}.models import"
            )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 2: repository.py — 타 모듈 repository import 금지 ---


def check_repository_no_cross_repo_import():
    """repository.py에서 타 모듈 repository를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.(\w+)\.repository")

    for repo_file in _MODULES_DIR.glob("*/repository.py"):
        own_module = _extract_module_name(repo_file)
        source = repo_file.read_text()
        tree = ast.parse(source, filename=str(repo_file))

        for node in ast.walk(tree):
            target_module = None
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if m:
                    target_module = m.group(1)
            if not target_module:
                target_module = _imports_submodule(node, "repository")
            if not target_module or target_module == own_module:
                continue
            rel = repo_file.relative_to(_ROOT)
            violations.append(
                f"  {rel}:{node.lineno} — {target_module}.repository import 금지"
            )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 3: api/ — repository 직접 import 금지 ---


def check_api_no_direct_repository_import():
    """api/ 레이어에서 modules의 repository를 직접 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.\w+\.repository")

    for py_file in _API_DIR.rglob("*.py"):
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            matched = False
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    matched = True
            if not matched:
                matched = _imports_submodule(node, "repository") is not None
            if matched:
                rel = py_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"api에서 repository 직접 import 금지 (service를 사용하세요)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 4: modules/ — api layer import 금지 ---


def check_modules_no_api_import():
    """modules/ 레이어에서 api를 import하면 위반 (역방향 의존)."""
    violations = []
    pattern = re.compile(r"app\.api\b")

    for py_file in _MODULES_DIR.rglob("*.py"):
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    rel = py_file.relative_to(_ROOT)
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"modules에서 api import 금지 (역방향 의존)"
                    )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 5: use_cases/ — infrastructure, repository import 금지 ---


def check_use_cases_no_infra_or_repo_import():
    """use_cases/에서 infrastructure 또는 repository를 직접 import하면 위반."""
    violations = []
    infra_pattern = re.compile(r"app\.infrastructure\b")
    repo_pattern = re.compile(r"app\.modules\.\w+\.repository")

    for py_file in _USE_CASES_DIR.rglob("*.py"):
        if py_file.name == "__init__.py":
            continue
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            rel = py_file.relative_to(_ROOT)
            for mod_name in _get_import_module_names(node):
                if infra_pattern.search(mod_name):
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"use_cases에서 infrastructure import 금지 (service를 사용하세요)"
                    )
                if repo_pattern.search(mod_name):
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"use_cases에서 repository import 금지 (service를 사용하세요)"
                    )
            if _imports_submodule(node, "repository") is not None:
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"use_cases에서 repository import 금지 (service를 사용하세요)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 6: service.py — 타 모듈 service import 금지 ---


def check_service_no_cross_service_import():
    """service.py에서 타 모듈 service를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.(\w+)\.service")

    for svc_file in _MODULES_DIR.glob("*/service.py"):
        own_module = _extract_module_name(svc_file)
        source = svc_file.read_text()
        tree = ast.parse(source, filename=str(svc_file))

        for node in ast.walk(tree):
            target_module = None
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if m:
                    target_module = m.group(1)
            if not target_module:
                target_module = _imports_submodule(node, "service")
            if not target_module or target_module == own_module:
                continue
            rel = svc_file.relative_to(_ROOT)
            violations.append(
                f"  {rel}:{node.lineno} — "
                f"{target_module}.service import 금지 (크로스 도메인은 use_case에서 조합)"
            )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 7: service.py — 타 모듈 repository import 금지 ---


def check_service_no_cross_repo_import():
    """service.py에서 타 모듈 repository를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.(\w+)\.repository")

    for svc_file in _MODULES_DIR.glob("*/service.py"):
        own_module = _extract_module_name(svc_file)
        source = svc_file.read_text()
        tree = ast.parse(source, filename=str(svc_file))

        for node in ast.walk(tree):
            target_module = None
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if m:
                    target_module = m.group(1)
            if not target_module:
                target_module = _imports_submodule(node, "repository")
            if not target_module or target_module == own_module:
                continue
            rel = svc_file.relative_to(_ROOT)
            violations.append(
                f"  {rel}:{node.lineno} — "
                f"{target_module}.repository import 금지 (자기 도메인 repo만 허용)"
            )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 8: queries/ — service import 금지 (repo만 호출) ---


def check_queries_no_service_import():
    """queries/에서 modules의 service를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.\w+\.service")

    for py_file in _QUERIES_DIR.rglob("*.py"):
        if py_file.name == "__init__.py":
            continue
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            matched = False
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    matched = True
            if not matched:
                matched = _imports_submodule(node, "service") is not None
            if matched:
                rel = py_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"queries에서 service import 금지 (repo를 직접 사용하세요)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 9: api/ — service 직접 import 금지 (queries/use_cases 경유) ---


def check_api_no_direct_service_import():
    """api/ 레이어에서 modules의 service를 직접 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.\w+\.service")

    for py_file in _API_DIR.rglob("*.py"):
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            matched = False
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    matched = True
            if not matched:
                matched = _imports_submodule(node, "service") is not None
            if matched:
                rel = py_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"api에서 service 직접 import 금지 (queries/use_cases를 사용하세요)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 10: queries/ — infrastructure import 금지 (repo만 호출) ---


def check_queries_no_infrastructure_import():
    """queries/에서 infrastructure를 직접 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.infrastructure\b")

    for py_file in _QUERIES_DIR.rglob("*.py"):
        if py_file.name == "__init__.py":
            continue
        rel = str(py_file.relative_to(_ROOT))
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"queries에서 infrastructure import 금지 (repo를 사용하세요)"
                    )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 11: handlers.py — service import 금지 (순환 의존 방지) ---


def check_handlers_no_service_import():
    """handlers.py에서 service를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.\w+\.service")

    for handler_file in _MODULES_DIR.glob("*/handlers.py"):
        source = handler_file.read_text()
        tree = ast.parse(source, filename=str(handler_file))

        for node in ast.walk(tree):
            matched = False
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    matched = True
            if not matched:
                matched = _imports_submodule(node, "service") is not None
            if matched:
                rel = handler_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"handlers에서 service import 금지 (순환 의존 방지)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 12: service.py — use_case import 금지 (역방향 의존) ---


def check_service_no_use_case_import():
    """service.py에서 use_cases를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.use_cases\b")

    for svc_file in _MODULES_DIR.glob("*/service.py"):
        source = svc_file.read_text()
        tree = ast.parse(source, filename=str(svc_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    rel = svc_file.relative_to(_ROOT)
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"service에서 use_cases import 금지 (역방향 의존)"
                    )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 13: repository.py — age_client 외 infrastructure import 금지 ---

# age_client는 DB 접근이므로 허용
_REPO_INFRA_ALLOWLIST = {"app.infrastructure.age_client"}


def check_repository_no_non_db_infra_import():
    """repository.py에서 age_client 외의 infrastructure를 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.infrastructure\.\w+")

    for repo_file in _MODULES_DIR.glob("*/repository.py"):
        source = repo_file.read_text()
        tree = ast.parse(source, filename=str(repo_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if not m:
                    continue
                infra_module = m.group(0)
                if infra_module in _REPO_INFRA_ALLOWLIST:
                    continue
                rel = repo_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"repository에서 {infra_module} import 금지 "
                    f"(age_client만 허용, URL 변환은 mapper로)"
                )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)


# --- 규칙 14: service.py — transactional 데코레이터 import 금지 ---


def check_service_no_transactional_import():
    """service.py에서 transactional을 import하면 위반 (트랜잭션은 use_case가 관리)."""
    violations = []
    pattern = re.compile(r"app\.core\.transactional")

    for svc_file in _MODULES_DIR.glob("*/service.py"):
        source = svc_file.read_text()
        tree = ast.parse(source, filename=str(svc_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    rel = svc_file.relative_to(_ROOT)
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"service에서 transactional import 금지 "
                        f"(트랜잭션 경계는 use_case가 관리)"
                    )

    if violations:
        pytest.fail("\n" + "\n".join(violations), pytrace=False)
