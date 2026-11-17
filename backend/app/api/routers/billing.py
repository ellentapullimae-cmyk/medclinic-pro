from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_pagination, require_role
from app.core.enums import InvoiceStatus, Role
from app.db.session import get_db
from app.models.billing import Invoice
from app.models.user import User
from app.schemas.billing import InvoiceCreate, InvoiceOut, PaymentIn
from app.schemas.common import Page
from app.services import billing_service

router = APIRouter(prefix="/invoices", tags=["billing"])


@router.get("", response_model=Page[InvoiceOut])
def list_invoices(
    status: InvoiceStatus | None = Query(None),
    patient_id: int | None = Query(None),
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    status_value = status.value if status else None
    return billing_service.list_invoices(db, status_value, patient_id, page, page_size)


@router.post("", response_model=InvoiceOut, status_code=201)
def create_invoice(
    body: InvoiceCreate,
    _: object = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Invoice:
    return billing_service.create_invoice(db, body)


@router.get("/{invoice_id}", response_model=InvoiceOut)
def get_invoice(
    invoice_id: int,
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Invoice:
    return billing_service.get_invoice(db, invoice_id)


@router.post("/{invoice_id}/payments", response_model=InvoiceOut)
def add_payment(
    invoice_id: int,
    body: PaymentIn,
    user: User = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Invoice:
    return billing_service.add_payment(db, invoice_id, body, user)


@router.post("/{invoice_id}/cancel", response_model=InvoiceOut)
def cancel_invoice(
    invoice_id: int,
    _: object = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> Invoice:
    return billing_service.cancel_invoice(db, invoice_id)
