"""활성화(Activation) API 스키마."""

from pydantic import BaseModel


class HealthCheckIssue(BaseModel):
    category: str
    severity: str
    message: str
    count: int


class HealthCheckResponse(BaseModel):
    total_nodes: int
    total_relationships: int
    node_counts: dict[str, int]
    relationship_counts: dict[str, int]
    issues: list[HealthCheckIssue]


class QueryRequest(BaseModel):
    question: str


class QueryResponse(BaseModel):
    cypher_query: str
    results: list[dict]
    answer: str


class StarterQuestion(BaseModel):
    question: str
    description: str


class StartersResponse(BaseModel):
    starters: list[StarterQuestion]
