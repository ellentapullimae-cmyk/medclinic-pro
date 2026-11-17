from pydantic import BaseModel, EmailStr, Field, field_validator


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=4)


class RegisterRequest(BaseModel):
    first_name: str = Field(min_length=2, max_length=80)
    last_name: str = Field(min_length=2, max_length=80)
    document_type: str = Field(min_length=2, max_length=20)
    document_number: str = Field(min_length=5, max_length=30)
    email: EmailStr
    phone: str | None = Field(default=None, max_length=30)
    password: str = Field(min_length=6, max_length=128)
    password_confirm: str = Field(min_length=6, max_length=128)
    accept_terms: bool = False

    @field_validator("document_type")
    @classmethod
    def normalize_doc_type(cls, v: str) -> str:
        allowed = {"dni", "pasaporte", "cedula", "cédula", "otro"}
        cleaned = v.strip().lower()
        if cleaned not in allowed:
            raise ValueError("Tipo de documento no válido")
        return "cédula" if cleaned == "cedula" else cleaned

    @field_validator("document_number", "phone")
    @classmethod
    def strip_digits(cls, v: str | None) -> str | None:
        if v is None:
            return None
        return v.strip()

    @field_validator("first_name", "last_name")
    @classmethod
    def strip_names(cls, v: str) -> str:
        return v.strip()

    @field_validator("accept_terms")
    @classmethod
    def must_accept(cls, v: bool) -> bool:
        if not v:
            raise ValueError("Debe aceptar los términos y condiciones")
        return v


class RegisterResponse(BaseModel):
    id: int
    email: EmailStr
    full_name: str
    role: str
    message: str


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str
    full_name: str
