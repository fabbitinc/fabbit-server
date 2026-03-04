"""사용량 조회 API 스키마."""

from enum import Enum

from pydantic import BaseModel


class StorageCategory(str, Enum):
    DRAWING = "drawing"
    ATTACHMENT = "attachment"
    OTHER = "other"


class StorageCategoryItem(BaseModel):
    category: StorageCategory
    bytes_used: int
    file_count: int


class StorageUsageResponse(BaseModel):
    bytes_used: int
    bytes_limit: int
    bytes_overage: int
    allow_overage: bool
    categories: list[StorageCategoryItem]
