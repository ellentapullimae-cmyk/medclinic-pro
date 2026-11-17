from fastapi import HTTPException
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.models.inventory import InventoryItem, StockMovement
from app.models.user import User
from app.schemas.common import Page
from app.schemas.inventory import (
    InventoryItemCreate,
    InventoryItemOut,
    InventoryItemUpdate,
    StockMovementIn,
    StockMovementOut,
)
from app.services.common import get_or_404, paginate


def list_items(db: Session, q: str | None, page: int, page_size: int) -> Page:
    stmt = select(InventoryItem).where(InventoryItem.is_active.is_(True)).order_by(InventoryItem.name)
    if q:
        pattern = f"%{q.strip()}%"
        stmt = stmt.where(or_(InventoryItem.name.ilike(pattern), InventoryItem.sku.ilike(pattern)))
    return paginate(db, stmt, page, page_size, InventoryItemOut)


def create_item(db: Session, data: InventoryItemCreate) -> InventoryItem:
    if db.scalar(select(InventoryItem).where(InventoryItem.sku == data.sku)):
        raise HTTPException(status_code=409, detail="Ya existe un ítem con ese SKU")
    item = InventoryItem(**data.model_dump())
    db.add(item)
    db.commit()
    db.refresh(item)
    return item


def update_item(db: Session, item_id: int, data: InventoryItemUpdate) -> InventoryItem:
    item = get_or_404(db, InventoryItem, item_id, "Ítem de inventario")
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(item, field, value)
    db.commit()
    db.refresh(item)
    return item


def register_movement(db: Session, item_id: int, data: StockMovementIn, user: User) -> StockMovement:
    item = get_or_404(db, InventoryItem, item_id, "Ítem de inventario")
    new_stock = item.stock + data.change
    if new_stock < 0:
        raise HTTPException(status_code=409, detail="Stock insuficiente para la salida solicitada")
    item.stock = new_stock
    movement = StockMovement(
        item_id=item.id,
        user_id=user.id,
        change=data.change,
        stock_after=new_stock,
        reason=data.reason,
    )
    db.add(movement)
    db.commit()
    db.refresh(movement)
    return movement


def list_movements(db: Session, item_id: int, page: int, page_size: int) -> Page:
    get_or_404(db, InventoryItem, item_id, "Ítem de inventario")
    stmt = select(StockMovement).where(StockMovement.item_id == item_id).order_by(StockMovement.id.desc())
    return paginate(db, stmt, page, page_size, StockMovementOut)
