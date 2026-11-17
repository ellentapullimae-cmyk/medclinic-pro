from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field

from app.core.enums import InvoiceStatus, PaymentMethod
from app.schemas.patient import PatientBrief


class InvoiceItemIn(BaseModel):
    concept: str = Field(min_length=2, max_length=150)
    quantity: int = Field(default=1, ge=1)
    unit_price: Decimal = Field(ge=0)


class InvoiceCreate(BaseModel):
    patient_id: int
    appointment_id: int | None = None
    items: list[InvoiceItemIn] = Field(min_length=1)


class InvoiceItemOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    concept: str
    quantity: int
    unit_price: Decimal
    subtotal: Decimal


class PaymentIn(BaseModel):
    amount: Decimal = Field(gt=0)
    method: PaymentMethod


class PaymentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    amount: Decimal
    method: PaymentMethod
    paid_at: datetime


class InvoiceOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    number: str
    patient_id: int
    appointment_id: int | None
    issue_date: date
    status: InvoiceStatus
    total: Decimal
    created_at: datetime
    patient: PatientBrief
    items: list[InvoiceItemOut]
    payments: list[PaymentOut]
