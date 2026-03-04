"""사용량 조회 API 스키마."""

from datetime import datetime
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


class CreditCategoryItem(BaseModel):
    category: str
    credits_used: int
    usage_count: int


class CreditUsageResponse(BaseModel):
    current_period_start: datetime
    current_period_end: datetime
    total_credits_used: int
    plan_credits_used: int
    plan_credits_limit: int
    plan_credits_remaining: int
    bonus_credits_used: int
    bonus_credits_remaining: int
    categories: list[CreditCategoryItem]
