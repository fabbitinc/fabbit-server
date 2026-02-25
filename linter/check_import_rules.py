"""아키텍처 Import 규칙 검증.

규칙:
1. models.py: 타 모듈 models import는 TYPE_CHECKING 내에서만
2. repository.py: 타 모듈 repository import 금지
3. api/: repository 직접 import 금지 (service만 호출)
4. modules/: api layer import 금지 (역방향 의존)
"""

import ast
import re
from pathlib import Path

_ROOT = Path(__file__).resolve().parent.parent
_MODULES_DIR = _ROOT / "app" / "modules"
_API_DIR = _ROOT / "app" / "api"


def _get_import_module_names(node: ast.AST) -> list[str]:
    """Import/ImportFrom 노드에서 모듈 경로를 추출."""
    if isinstance(node, ast.Import):
        return [alias.name for alias in node.names]
    if isinstance(node, ast.ImportFrom) and node.module:
        return [node.module]
    return []


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
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if not m:
                    continue
                target_module = m.group(1)
                if target_module == own_module:
                    continue
                if _is_inside_type_checking(node, tree):
                    continue
                rel = models_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"TYPE_CHECKING 밖에서 {target_module}.models import"
                )

    assert not violations, (
        "models.py 타 모듈 models import 위반 (TYPE_CHECKING 내에서만 허용):\n"
        + "\n".join(violations)
    )


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
            for mod_name in _get_import_module_names(node):
                m = pattern.search(mod_name)
                if not m:
                    continue
                target_module = m.group(1)
                if target_module == own_module:
                    continue
                rel = repo_file.relative_to(_ROOT)
                violations.append(
                    f"  {rel}:{node.lineno} — "
                    f"{target_module}.repository import 금지"
                )

    assert not violations, (
        "repository.py 타 모듈 repository import 위반:\n" + "\n".join(violations)
    )


# --- 규칙 3: api/ — repository 직접 import 금지 ---


def check_api_no_direct_repository_import():
    """api/ 레이어에서 modules의 repository를 직접 import하면 위반."""
    violations = []
    pattern = re.compile(r"app\.modules\.\w+\.repository")

    for py_file in _API_DIR.rglob("*.py"):
        source = py_file.read_text()
        tree = ast.parse(source, filename=str(py_file))

        for node in ast.walk(tree):
            for mod_name in _get_import_module_names(node):
                if pattern.search(mod_name):
                    rel = py_file.relative_to(_ROOT)
                    violations.append(
                        f"  {rel}:{node.lineno} — "
                        f"api에서 repository 직접 import 금지 (service를 사용하세요)"
                    )

    assert not violations, (
        "api/ 레이어 repository import 위반:\n" + "\n".join(violations)
    )


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

    assert not violations, (
        "modules/ → api/ 역방향 import 위반:\n" + "\n".join(violations)
    )
