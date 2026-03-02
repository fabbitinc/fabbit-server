"""Team 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.team.get_team_detail import get_team_detail
from app.queries.team.list_members import list_members
from app.queries.team.list_teams import list_teams
from app.queries.team.lookup_teams import lookup_teams

__all__ = [
    "get_team_detail",
    "list_members",
    "list_teams",
    "lookup_teams",
]
