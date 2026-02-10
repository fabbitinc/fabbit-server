"""SQLAlchemy Unit of Work 구현."""

from types import TracebackType

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from src.adapters.outbound.persistence.account.adapter import (
    AccountPersistenceAdapter,
    RefreshTokenPersistenceAdapter,
)
from src.adapters.outbound.persistence.ai.adapter import AiUsagePersistenceAdapter
from src.adapters.outbound.persistence.bom.adapter import BomEntryPersistenceAdapter
from src.adapters.outbound.persistence.attribute.adapter import (
    AttributeDefinitionPersistenceAdapter,
    AttributeValuePersistenceAdapter,
)
from src.adapters.outbound.persistence.file.adapter import FilePersistenceAdapter
from src.adapters.outbound.persistence.folder.adapter import FolderPersistenceAdapter
from src.adapters.outbound.persistence.item.adapter import (
    ItemPersistenceAdapter,
    ItemRevisionPersistenceAdapter,
)
from src.adapters.outbound.persistence.organization.adapter import (
    OrganizationPersistenceAdapter,
)
from src.adapters.outbound.persistence.project.adapter import ProjectPersistenceAdapter
from src.application._shared.auth_context import get_auth_context_or_none
from src.application._shared.unit_of_work import UnitOfWork


class SQLAlchemyUnitOfWork(UnitOfWork):
    """SQLAlchemy 기반 Unit of Work 구현."""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def __aenter__(self) -> "SQLAlchemyUnitOfWork":
        self._session = self._session_factory()

        ctx = get_auth_context_or_none()
        if ctx and ctx.organization_id:
            # asyncpg는 SET LOCAL에 바인딩 파라미터 미지원, f-string 사용 (UUID는 안전)
            await self._session.execute(
                text(f"SET LOCAL app.current_org_id = '{ctx.organization_id}'")
            )

        self.accounts = AccountPersistenceAdapter(self._session)
        self.refresh_tokens = RefreshTokenPersistenceAdapter(self._session)
        self.organizations = OrganizationPersistenceAdapter(self._session)
        self.projects = ProjectPersistenceAdapter(self._session)
        self.folders = FolderPersistenceAdapter(self._session)
        self.items = ItemPersistenceAdapter(self._session)
        self.item_revisions = ItemRevisionPersistenceAdapter(self._session)
        self.files = FilePersistenceAdapter(self._session)
        self.attribute_definitions = AttributeDefinitionPersistenceAdapter(self._session)
        self.attribute_values = AttributeValuePersistenceAdapter(self._session)
        self.bom_entries = BomEntryPersistenceAdapter(self._session)
        self.ai_usages = AiUsagePersistenceAdapter(self._session)
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: TracebackType | None,
    ) -> None:
        if exc_type is not None:
            await self.rollback()
        await self._session.close()

    async def commit(self) -> None:
        await self._session.commit()

    async def rollback(self) -> None:
        await self._session.rollback()
