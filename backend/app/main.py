from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app.api import api_router
from app.core.config import settings
from app.db.session import Base, SessionLocal, engine
from app.db.seed import seed_admin


def _ensure_user_columns() -> None:
    """Agrega columnas de registro si la tabla users ya existe sin ellas."""
    required = {
        "document_type": "ALTER TABLE users ADD COLUMN document_type VARCHAR(20) NULL",
        "document_number": "ALTER TABLE users ADD COLUMN document_number VARCHAR(30) NULL",
        "phone": "ALTER TABLE users ADD COLUMN phone VARCHAR(30) NULL",
    }
    try:
        with engine.connect() as conn:
            rows = conn.execute(text("SHOW COLUMNS FROM users")).fetchall()
            existing = {row[0] for row in rows}
            added_doc = False
            for col, ddl in required.items():
                if col not in existing:
                    conn.execute(text(ddl))
                    if col == "document_number":
                        added_doc = True
            if added_doc:
                conn.execute(
                    text(
                        "CREATE UNIQUE INDEX ix_users_document_number ON users (document_number)"
                    )
                )
            conn.commit()
    except Exception:
        engine.dispose()


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)
    _ensure_user_columns()
    db = SessionLocal()
    try:
        seed_admin(db)
    finally:
        db.close()
    yield


app = FastAPI(title=settings.app_name, version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix=settings.api_v1)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": settings.app_name}
