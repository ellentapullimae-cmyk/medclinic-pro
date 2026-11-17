from math import ceil
from typing import TypeVar

from fastapi import HTTPException, status
from sqlalchemy import Select, func, select
from sqlalchemy.orm import Session

from app.schemas.common import Page

ModelType = TypeVar("ModelType")


def get_or_404(db: Session, model, entity_id: int, name: str = "recurso"):
    obj = db.get(model, entity_id)
    if obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f"{name} no encontrado")
    return obj


def paginate(db: Session, stmt: Select, page: int, page_size: int, item_model) -> Page:
    total = db.scalar(select(func.count()).select_from(stmt.order_by(None).subquery())) or 0
    rows = db.scalars(stmt.offset((page - 1) * page_size).limit(page_size)).all()
    return Page[item_model](
        items=rows,
        total=total,
        page=page,
        pages=max(1, ceil(total / page_size)),
    )
