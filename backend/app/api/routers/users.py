from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, require_role
from app.core.enums import Role
from app.core.security import hash_password
from app.db.session import get_db
from app.models.user import User
from app.schemas.user import UserBrief, UserCreate, UserOut, UserUpdate

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/doctors", response_model=list[UserBrief])
def list_doctors(
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[User]:
    return (
        db.scalars(
            select(User)
            .where(User.role.in_(["medico", "admin"]), User.is_active.is_(True))
            .order_by(User.full_name)
        )
        .all()
    )


@router.get("/me", response_model=UserOut)
def me(user: User = Depends(get_current_user)) -> User:
    return user


@router.get("", response_model=list[UserOut])
def list_users(
    _: User = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> list[User]:
    return db.scalars(select(User).order_by(User.full_name)).all()


@router.post("", response_model=UserOut, status_code=201)
def create_user(
    body: UserCreate,
    _: User = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> User:
    email = body.email.lower()
    if db.scalar(select(User).where(User.email == email)):
        raise HTTPException(status_code=409, detail="Ese correo ya está registrado")
    user = User(
        email=email,
        password_hash=hash_password(body.password),
        full_name=body.full_name,
        role=body.role.value,
        specialty=body.specialty,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.patch("/{user_id}", response_model=UserOut)
def update_user(
    user_id: int,
    body: UserUpdate,
    _: User = Depends(require_role(Role.admin)),
    db: Session = Depends(get_db),
) -> User:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    payload = body.model_dump(exclude_unset=True)
    if "role" in payload and payload["role"] is not None:
        payload["role"] = payload["role"].value
    if payload.get("password"):
        payload["password_hash"] = hash_password(payload.pop("password"))
    for field, value in payload.items():
        setattr(user, field, value)
    db.commit()
    db.refresh(user)
    return user
