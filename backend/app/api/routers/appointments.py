from datetime import date as date_type

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_pagination, require_role
from app.core.enums import AppointmentStatus, Role
from app.db.session import get_db
from app.models.appointment import Appointment
from app.schemas.appointment import AppointmentCreate, AppointmentOut, AppointmentUpdate
from app.schemas.common import Page
from app.services import appointment_service

router = APIRouter(prefix="/appointments", tags=["appointments"])


@router.get("", response_model=Page[AppointmentOut])
def list_appointments(
    date: date_type | None = Query(None),
    doctor_id: int | None = Query(None),
    status: AppointmentStatus | None = Query(None),
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    status_value = status.value if status else None
    return appointment_service.list_appointments(db, date, doctor_id, status_value, page, page_size)


@router.post("", response_model=AppointmentOut, status_code=201)
def create_appointment(
    body: AppointmentCreate,
    _: object = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Appointment:
    return appointment_service.create_appointment(db, body)


@router.patch("/{appointment_id}", response_model=AppointmentOut)
def update_appointment(
    appointment_id: int,
    body: AppointmentUpdate,
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Appointment:
    return appointment_service.update_appointment(db, appointment_id, body)


@router.delete("/{appointment_id}", response_model=AppointmentOut)
def cancel_appointment(
    appointment_id: int,
    _: object = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Appointment:
    return appointment_service.cancel_appointment(db, appointment_id)
