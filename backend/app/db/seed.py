from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.user import User

ADMIN_EMAIL = "admin@clinica.com"
ADMIN_PASSWORD = "admin123"


def seed_admin(db: Session) -> None:
    exists = db.scalar(select(User).where(User.email == ADMIN_EMAIL))
    if exists:
        return
    db.add(
        User(
            email=ADMIN_EMAIL,
            password_hash=hash_password(ADMIN_PASSWORD),
            full_name="Administrador",
            role="admin",
            is_active=True,
        )
    )
    db.commit()
