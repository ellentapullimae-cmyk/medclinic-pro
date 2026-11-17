from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field

from app.core.enums import ItemCategory
from app.schemas.user import UserBrief


class InventoryItemCreate(BaseModel):
    sku: str = Field(min_length=1, max_length=40)
    name: str = Field(min_length=2, max_length=150)
    category: ItemCategory = ItemCategory.insumo
    unit: str = "unidad"
    stock: int = Field(default=0, ge=0)
    min_stock: int = Field(default=0, ge=0)
    expiry_date: date | None = None
    unit_cost: Decimal = Field(default=Decimal("0.00"), ge=0)


class InventoryItemUpdate(BaseModel):
    name: str | None = None
    unit: str | None = None
    min_stock: int | None = Field(default=None, ge=0)
    expiry_date: date | None = None
    unit_cost: Decimal | None = Field(default=None, ge=0)
    is_active: bool | None = None


class InventoryItemOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    sku: str
    name: str
    category: ItemCategory
    unit: str
    stock: int
    min_stock: int
    low_stock: bool
    expiry_date: date | None
    unit_cost: Decimal
    is_active: bool
    created_at: datetime


class StockMovementIn(BaseModel):
    change: int = Field(description="Positivo = entrada, negativo = salida")
    reason: str | None = None


class StockMovementOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    item_id: int
    change: int
    stock_after: int
    reason: str | None
    created_at: datetime
    user: UserBrief | None = None
