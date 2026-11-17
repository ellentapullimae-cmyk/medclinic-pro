from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_pagination, require_role
from app.core.enums import Role
from app.db.session import get_db
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
from app.services import inventory_service

router = APIRouter(prefix="/inventory", tags=["inventory"])


@router.get("", response_model=Page[InventoryItemOut])
def list_items(
    q: str | None = Query(None),
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    return inventory_service.list_items(db, q, page, page_size)


@router.post("", response_model=InventoryItemOut, status_code=201)
def create_item(
    body: InventoryItemCreate,
    _: object = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> InventoryItem:
    return inventory_service.create_item(db, body)


@router.patch("/{item_id}", response_model=InventoryItemOut)
def update_item(
    item_id: int,
    body: InventoryItemUpdate,
    _: object = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> InventoryItem:
    return inventory_service.update_item(db, item_id, body)


@router.post("/{item_id}/movements", response_model=StockMovementOut, status_code=201)
def register_movement(
    item_id: int,
    body: StockMovementIn,
    user: User = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
) -> StockMovement:
    return inventory_service.register_movement(db, item_id, body, user)


@router.get("/{item_id}/movements", response_model=Page[StockMovementOut])
def list_movements(
    item_id: int,
    _: object = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    return inventory_service.list_movements(db, item_id, page, page_size)
