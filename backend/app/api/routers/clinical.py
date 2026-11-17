from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_pagination, require_role
from app.core.enums import Role
from app.db.session import get_db
from app.models.clinical import ClinicalRecord
from app.models.user import User
from app.schemas.clinical import ClinicalRecordCreate, ClinicalRecordOut
from app.schemas.common import Page
from app.services import clinical_service

router = APIRouter(tags=["clinical"])


@router.get("/patients/{patient_id}/records", response_model=Page[ClinicalRecordOut])
def list_records(
    patient_id: int,
    user: User = Depends(require_role(Role.medico, Role.admin)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    return clinical_service.list_records(db, patient_id, page, page_size)


@router.post("/patients/{patient_id}/records", response_model=ClinicalRecordOut, status_code=201)
def create_record(
    patient_id: int,
    body: ClinicalRecordCreate,
    user: User = Depends(require_role(Role.medico)),
    db: Session = Depends(get_db),
) -> ClinicalRecord:
    return clinical_service.create_record(db, patient_id, body, user)


@router.get("/clinical-records/{record_id}", response_model=ClinicalRecordOut)
def get_record(
    record_id: int,
    _: object = Depends(require_role(Role.medico, Role.admin)),
    db: Session = Depends(get_db),
) -> ClinicalRecord:
    return clinical_service.get_record(db, record_id)
