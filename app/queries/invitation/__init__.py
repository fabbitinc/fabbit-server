"""Invitation 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.invitation.list_invitations import list_invitations

__all__ = [
    "list_invitations",
]
