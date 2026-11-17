from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.models.patient import Patient
from app.schemas.common import Page
from app.schemas.patient import PatientCreate, PatientOut, PatientUpdate
from app.services.common import get_or_404, paginate


def create_patient(db: Session, data: PatientCreate) -> Patient:
    dni_taken = db.scalar(select(Patient).where(Patient.dni == data.dni))
    if dni_taken:
        from fastapi import HTTPException

        raise HTTPException(status_code=409, detail="Ya existe un paciente con ese DNI")
    last_id = db.scalar(select(func.max(Patient.id))) or 0
    patient = Patient(**data.model_dump(), file_number=f"HC-{last_id + 1:06d}")
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


def list_patients(db: Session, q: str | None, page: int, page_size: int) -> Page:
    stmt = select(Patient).order_by(Patient.last_name, Patient.first_name)
    if q:
        pattern = f"%{q.strip()}%"
        stmt = stmt.where(
            or_(
                Patient.first_name.ilike(pattern),
                Patient.last_name.ilike(pattern),
                Patient.dni.ilike(pattern),
                Patient.file_number.ilike(pattern),
            )
        )
    return paginate(db, stmt, page, page_size, PatientOut)


def get_patient(db: Session, patient_id: int) -> Patient:
    return get_or_404(db, Patient, patient_id, "Paciente")


def update_patient(db: Session, patient_id: int, data: PatientUpdate) -> Patient:
    patient = get_patient(db, patient_id)
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(patient, field, value)
    db.commit()
    db.refresh(patient)
    return patient


def deactivate_patient(db: Session, patient_id: int) -> Patient:
    patient = get_patient(db, patient_id)
    patient.is_active = False
    db.commit()
    db.refresh(patient)
    return patient
