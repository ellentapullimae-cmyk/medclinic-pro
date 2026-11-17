from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.schemas.patient import PatientBrief
from app.schemas.user import UserBrief


class ClinicalRecordCreate(BaseModel):
    diagnosis: str | None = None
    treatment: str | None = None
    notes: str | None = None
    vitals: str | None = None
    visit_date: datetime | None = None


class ClinicalRecordOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    patient_id: int
    doctor_id: int
    visit_date: datetime
    diagnosis: str | None
    treatment: str | None
    notes: str | None
    vitals: str | None
    created_at: datetime
    patient: PatientBrief
    doctor: UserBrief
