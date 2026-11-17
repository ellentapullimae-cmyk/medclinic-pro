from datetime import date as date_type

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.appointment import Appointment
from app.models.patient import Patient
from app.models.user import User
from app.schemas.appointment import AppointmentCreate, AppointmentOut, AppointmentUpdate
from app.schemas.common import Page
from app.services.common import get_or_404, paginate

ACTIVE_STATUSES = ("pendiente", "confirmada")


def _check_conflicts(db: Session, doctor_id: int, start, end, exclude_id: int | None = None) -> None:
    stmt = select(Appointment).where(
        Appointment.doctor_id == doctor_id,
        Appointment.status.in_(ACTIVE_STATUSES),
        Appointment.start_time < end,
        Appointment.end_time > start,
    )
    if exclude_id is not None:
        stmt = stmt.where(Appointment.id != exclude_id)
    if db.scalar(stmt):
        raise HTTPException(status_code=409, detail="El médico ya tiene una cita en ese horario")


def create_appointment(db: Session, data: AppointmentCreate) -> Appointment:
    patient = get_or_404(db, Patient, data.patient_id, "Paciente")
    doctor = get_or_404(db, User, data.doctor_id, "Médico")
    if not doctor.is_active:
        raise HTTPException(status_code=400, detail="El médico está inactivo")
    if patient.is_active is False:
        raise HTTPException(status_code=400, detail="El paciente está dado de baja")
    _check_conflicts(db, data.doctor_id, data.start_time, data.end_time)
    appointment = Appointment(**data.model_dump())
    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment


def list_appointments(
    db: Session,
    day: date_type | None,
    doctor_id: int | None,
    status_filter: str | None,
    page: int,
    page_size: int,
) -> Page:
    stmt = select(Appointment).order_by(Appointment.start_time)
    if day:
        stmt = stmt.where(Appointment.start_time >= datetime_start(day), Appointment.start_time < datetime_end(day))
    if doctor_id:
        stmt = stmt.where(Appointment.doctor_id == doctor_id)
    if status_filter:
        stmt = stmt.where(Appointment.status == status_filter)
    return paginate(db, stmt, page, page_size, AppointmentOut)


def datetime_start(day: date_type):
    from datetime import datetime, time

    return datetime.combine(day, time.min)


def datetime_end(day: date_type):
    from datetime import datetime, time

    return datetime.combine(day, time.max)


def update_appointment(db: Session, appointment_id: int, data: AppointmentUpdate) -> Appointment:
    appointment = get_or_404(db, Appointment, appointment_id, "Cita")
    payload = data.model_dump(exclude_unset=True)
    new_start = payload.get("start_time", appointment.start_time)
    new_end = payload.get("end_time", appointment.end_time)
    if new_end <= new_start:
        raise HTTPException(status_code=400, detail="end_time debe ser posterior a start_time")
    status_value = payload.get("status", appointment.status)
    if status_value in ACTIVE_STATUSES and (
        "start_time" in payload or "end_time" in payload or appointment.status in ACTIVE_STATUSES
    ):
        _check_conflicts(db, appointment.doctor_id, new_start, new_end, exclude_id=appointment.id)
    for field, value in payload.items():
        setattr(appointment, field, value)
    db.commit()
    db.refresh(appointment)
    return appointment


def cancel_appointment(db: Session, appointment_id: int) -> Appointment:
    appointment = get_or_404(db, Appointment, appointment_id, "Cita")
    appointment.status = "cancelada"
    db.commit()
    db.refresh(appointment)
    return appointment
