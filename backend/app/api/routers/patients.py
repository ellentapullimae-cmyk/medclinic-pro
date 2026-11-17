from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_pagination, require_role
from app.core.enums import Role
from app.db.session import get_db
from app.models.patient import Patient
from app.schemas.common import Page
from app.schemas.patient import PatientCreate, PatientOut, PatientUpdate
from app.services import patient_service

router = APIRouter(prefix="/patients", tags=["patients"])


@router.get("", response_model=Page[PatientOut])
def list_patients(
    q: str | None = Query(None, description="Nombre, DNI o nº de historia"),
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
    pagination: tuple[int, int] = Depends(get_pagination),
) -> Page:
    page, page_size = pagination
    return patient_service.list_patients(db, q, page, page_size)


@router.post("", response_model=PatientOut, status_code=201)
def create_patient(
    body: PatientCreate,
    _: object = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Patient:
    return patient_service.create_patient(db, body)


@router.get("/{patient_id}", response_model=PatientOut)
def get_patient(
    patient_id: int,
    _: object = Depends(require_role(Role.admin, Role.medico, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Patient:
    return patient_service.get_patient(db, patient_id)


@router.patch("/{patient_id}", response_model=PatientOut)
def update_patient(
    patient_id: int,
    body: PatientUpdate,
    _: object = Depends(require_role(Role.admin, Role.recepcion)),
    db: Session = Depends(get_db),
) -> Patient:
    return patient_service.update_patient(db, patient_id, body)


@router.delete("/{patient_id}", response_model=PatientOut)
def deactivate_patient(
    patient_id: int,
    _: object = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> Patient:
    return patient_service.deactivate_patient(db, patient_id)
