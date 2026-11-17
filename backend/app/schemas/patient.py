from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field

from app.core.enums import Sex


class PatientBase(BaseModel):
    first_name: str = Field(min_length=2, max_length=80)
    last_name: str = Field(min_length=2, max_length=80)
    dni: str = Field(min_length=3, max_length=20)
    birth_date: date | None = None
    sex: Sex | None = None
    phone: str | None = None
    email: EmailStr | None = None
    address: str | None = None
    insurance: str | None = None
    allergies: str | None = None
    notes: str | None = None


class PatientCreate(PatientBase):
    pass


class PatientUpdate(BaseModel):
    first_name: str | None = None
    last_name: str | None = None
    phone: str | None = None
    email: EmailStr | None = None
    address: str | None = None
    insurance: str | None = None
    allergies: str | None = None
    notes: str | None = None
    is_active: bool | None = None


class PatientBrief(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    file_number: str
    first_name: str
    last_name: str


class PatientOut(PatientBrief):
    model_config = ConfigDict(from_attributes=True)

    dni: str
    birth_date: date | None
    sex: Sex | None
    phone: str | None
    email: EmailStr | None
    address: str | None
    insurance: str | None
    allergies: str | None
    notes: str | None
    is_active: bool
    created_at: datetime
