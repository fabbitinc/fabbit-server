"""TipTap JSON에서 mention 노드를 추출하는 유틸리티."""

import json
from uuid import UUID


def extract_mentions(body: str | None) -> tuple[set[UUID], set[UUID]]:
    """TipTap JSON body에서 (user_ids, issue_ids)를 추출한다.

    userMention → user_ids, issueMention → issue_ids.
    body가 None이거나 파싱 실패 시 빈 집합을 반환한다.
    """
    if not body:
        return set(), set()
    try:
        doc = json.loads(body)
    except (json.JSONDecodeError, TypeError):
        return set(), set()

    user_ids: set[UUID] = set()
    issue_ids: set[UUID] = set()
    _walk(doc, user_ids, issue_ids)
    return user_ids, issue_ids


def _walk(node: dict, user_ids: set[UUID], issue_ids: set[UUID]) -> None:
    """TipTap 노드 트리를 재귀 탐색하여 mention attrs.id를 수집."""
    node_type = node.get("type")
    attrs = node.get("attrs")
    if node_type in ("userMention", "issueMention") and attrs:
        try:
            uid = UUID(str(attrs["id"]))
        except (KeyError, ValueError):
            pass
        else:
            if node_type == "userMention":
                user_ids.add(uid)
            else:
                issue_ids.add(uid)

    for child in node.get("content") or []:
        if isinstance(child, dict):
            _walk(child, user_ids, issue_ids)
