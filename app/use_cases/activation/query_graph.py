"""Activation 자연어 질의 — LLM 호출 + 이벤트 발행 오케스트레이션."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.activation import service
from app.modules.activation.schemas import QueryResponse


@transactional(read_only=True)
def query_graph(db: Session, auth: AuthContext, question: str) -> QueryResponse:
    """자연어 질문을 그래프/SQL 조회로 변환해 결과를 반환합니다."""
    return service.query_graph(db, auth, question)
