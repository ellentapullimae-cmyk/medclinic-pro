from decimal import Decimal

from fastapi import HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.billing import Invoice, InvoiceItem, Payment
from app.models.patient import Patient
from app.models.user import User
from app.schemas.billing import InvoiceCreate, InvoiceOut, PaymentIn
from app.schemas.common import Page
from app.services.common import get_or_404, paginate


def create_invoice(db: Session, data: InvoiceCreate) -> Invoice:
    get_or_404(db, Patient, data.patient_id, "Paciente")
    last_id = db.scalar(select(func.max(Invoice.id))) or 0
    items = [
        InvoiceItem(
            concept=item.concept,
            quantity=item.quantity,
            unit_price=item.unit_price,
            subtotal=item.unit_price * item.quantity,
        )
        for item in data.items
    ]
    total = sum((item.subtotal for item in items), Decimal("0.00"))
    invoice = Invoice(
        number=f"FAC-{last_id + 1:06d}",
        patient_id=data.patient_id,
        appointment_id=data.appointment_id,
        total=total,
    )
    invoice.items = items
    db.add(invoice)
    db.commit()
    db.refresh(invoice)
    return invoice


def list_invoices(db: Session, status_filter: str | None, patient_id: int | None, page: int, page_size: int) -> Page:
    stmt = select(Invoice).order_by(Invoice.id.desc())
    if status_filter:
        stmt = stmt.where(Invoice.status == status_filter)
    if patient_id:
        stmt = stmt.where(Invoice.patient_id == patient_id)
    return paginate(db, stmt, page, page_size, InvoiceOut)


def get_invoice(db: Session, invoice_id: int) -> Invoice:
    return get_or_404(db, Invoice, invoice_id, "Factura")


def add_payment(db: Session, invoice_id: int, data: PaymentIn, cashier: User) -> Invoice:
    invoice = get_invoice(db, invoice_id)
    if invoice.status == "anulada":
        raise HTTPException(status_code=409, detail="No se puede pagar una factura anulada")
    already_paid = sum((p.amount for p in invoice.payments), Decimal("0.00"))
    if already_paid + data.amount > invoice.total:
        raise HTTPException(status_code=409, detail="El pago supera el saldo pendiente de la factura")
    db.add(Payment(invoice_id=invoice.id, cashier_id=cashier.id, amount=data.amount, method=data.method.value))
    if already_paid + data.amount >= invoice.total:
        invoice.status = "pagada"
    db.commit()
    db.refresh(invoice)
    return invoice


def cancel_invoice(db: Session, invoice_id: int) -> Invoice:
    invoice = get_invoice(db, invoice_id)
    if invoice.payments:
        raise HTTPException(status_code=409, detail="No se puede anular una factura con pagos")
    invoice.status = "anulada"
    db.commit()
    db.refresh(invoice)
    return invoice
