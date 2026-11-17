from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.enums import Role
from app.core.security import create_access_token, hash_password, verify_password
from app.db.session import get_db
from app.models.patient import Patient
from app.models.user import User
from app.schemas.auth import LoginRequest, RegisterRequest, RegisterResponse, Token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=Token)
def login(body: LoginRequest, db: Session = Depends(get_db)) -> Token:
    user = db.scalar(select(User).where(User.email == body.email.lower()))
    if user is None or not user.is_active or not verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Credenciales incorrectas")
    token = create_access_token(user.id, user.role)
    return Token(access_token=token, role=user.role, full_name=user.full_name)


@router.post("/register", response_model=RegisterResponse, status_code=201)
def register_public(body: RegisterRequest, db: Session = Depends(get_db)) -> RegisterResponse:
    email = body.email.lower().strip()
    doc_number = body.document_number.strip()

    if db.scalar(select(User).where(User.email == email)):
        raise HTTPException(status_code=409, detail="Ese correo ya está registrado")

    if db.scalar(select(User).where(User.document_number == doc_number)):
        raise HTTPException(status_code=409, detail="Ese documento ya está registrado")

    if db.scalar(select(Patient).where(Patient.dni == doc_number)):
        raise HTTPException(status_code=409, detail="Ese documento ya tiene ficha de paciente")

    if body.password != body.password_confirm:
        raise HTTPException(status_code=422, detail="Las contraseñas no coinciden")

    full_name = f"{body.first_name.strip()} {body.last_name.strip()}".strip()

    user = User(
        email=email,
        password_hash=hash_password(body.password),
        full_name=full_name,
        role=Role.paciente.value,
        document_type=body.document_type,
        document_number=doc_number,
        phone=body.phone,
        is_active=True,
    )
    db.add(user)
    db.flush()

    last_id = db.scalar(select(func.max(Patient.id))) or 0
    patient = Patient(
        file_number=f"HC-{last_id + 1:06d}",
        first_name=body.first_name.strip(),
        last_name=body.last_name.strip(),
        dni=doc_number,
        phone=body.phone,
        email=email,
        is_active=True,
    )
    db.add(patient)
    db.commit()
    db.refresh(user)

    return RegisterResponse(
        id=user.id,
        email=user.email,
        full_name=user.full_name,
        role=user.role,
        message="Cuenta creada correctamente. Ya puede iniciar sesión.",
    )
