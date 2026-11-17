from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.clinical import ClinicalRecord
from app.models.patient import Patient
from app.models.user import User
from app.schemas.clinical import ClinicalRecordCreate, ClinicalRecordOut
from app.schemas.common import Page
from app.services.common import get_or_404, paginate


def create_record(db: Session, patient_id: int, data: ClinicalRecordCreate, doctor: User) -> ClinicalRecord:
    get_or_404(db, Patient, patient_id, "Paciente")
    record = ClinicalRecord(
        patient_id=patient_id,
        doctor_id=doctor.id,
        diagnosis=data.diagnosis,
        treatment=data.treatment,
        notes=data.notes,
        vitals=data.vitals,
    )
    if data.visit_date is not None:
        record.visit_date = data.visit_date
    db.add(record)
    db.commit()
    db.refresh(record)
    return record


def list_records(db: Session, patient_id: int, page: int, page_size: int) -> Page:
    get_or_404(db, Patient, patient_id, "Paciente")
    stmt = (
        select(ClinicalRecord)
        .where(ClinicalRecord.patient_id == patient_id)
        .order_by(ClinicalRecord.visit_date.desc())
    )
    return paginate(db, stmt, page, page_size, ClinicalRecordOut)


def get_record(db: Session, record_id: int) -> ClinicalRecord:
    record = db.scalar(select(ClinicalRecord).where(ClinicalRecord.id == record_id))
    if record is None:
        raise HTTPException(status_code=404, detail="Registro clínico no encontrado")
    return record
