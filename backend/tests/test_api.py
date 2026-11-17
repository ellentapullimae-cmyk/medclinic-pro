import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="module")
def client() -> TestClient:
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture()
def admin_token(client: TestClient) -> str:
    response = client.post("/api/v1/auth/login", json={"email": "admin@clinica.com", "password": "admin123"})
    assert response.status_code == 200, response.text
    return response.json()["access_token"]


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def test_health(client: TestClient):
    assert client.get("/health").json()["status"] == "ok"


def test_login_and_me(client: TestClient, admin_token: str):
    me = client.get("/api/v1/users/me", headers=auth_headers(admin_token))
    assert me.status_code == 200
    assert me.json()["role"] == "admin"


def test_login_wrong_password(client: TestClient):
    response = client.post("/api/v1/auth/login", json={"email": "admin@clinica.com", "password": "mala"})
    assert response.status_code == 401


def test_public_register_paciente_and_login(client: TestClient):
    import secrets

    suffix = secrets.token_hex(4)
    email = f"pac.test.{suffix}@example.com"
    doc = f"9{suffix[:8]}"

    payload = {
        "first_name": "Lucia",
        "last_name": "Gomez",
        "document_type": "dni",
        "document_number": doc,
        "email": email,
        "phone": "1155550000",
        "password": "secret123",
        "password_confirm": "secret123",
        "accept_terms": True,
    }
    created = client.post("/api/v1/auth/register", json=payload)
    assert created.status_code == 201, created.text
    body = created.json()
    assert body["role"] == "paciente"
    assert body["email"] == email

    dup = client.post("/api/v1/auth/register", json=payload)
    assert dup.status_code == 409

    wrong_terms = {**payload, "email": f"other.{suffix}@example.com", "document_number": f"8{suffix[:8]}", "accept_terms": False}
    rejected = client.post("/api/v1/auth/register", json=wrong_terms)
    assert rejected.status_code == 422

    login = client.post("/api/v1/auth/login", json={"email": email, "password": "secret123"})
    assert login.status_code == 200, login.text
    assert login.json()["role"] == "paciente"

    me = client.get(
        "/api/v1/users/me",
        headers=auth_headers(login.json()["access_token"]),
    )
    assert me.status_code == 200
    assert me.json()["role"] == "paciente"
    assert me.json()["document_number"] == doc

    admin_users = client.get(
        "/api/v1/users",
        headers=auth_headers(client.post(
            "/api/v1/auth/login",
            json={"email": "admin@clinica.com", "password": "admin123"},
        ).json()["access_token"]),
    )
    assert admin_users.status_code == 200
    emails = [u["email"] for u in admin_users.json()]
    assert email in emails


def test_register_password_hashed_in_mysql(client: TestClient):
    import secrets

    from app.db.session import SessionLocal
    from sqlalchemy import text

    suffix = secrets.token_hex(4)
    email = f"hash.test.{suffix}@example.com"
    password = "HashMe#123"
    doc = f"7{suffix[:8]}"

    created = client.post(
        "/api/v1/auth/register",
        json={
            "first_name": "Hash",
            "last_name": "Test",
            "document_type": "dni",
            "document_number": doc,
            "email": email,
            "password": password,
            "password_confirm": password,
            "accept_terms": True,
        },
    )
    assert created.status_code == 201, created.text

    db = SessionLocal()
    try:
        row = db.execute(
            text("SELECT password_hash, role FROM users WHERE email = :e"),
            {"e": email},
        ).fetchone()
        assert row is not None
        password_hash, role = row[0], row[1]
        assert role == "paciente"
        assert password_hash != password
        assert password_hash.startswith("$2b$") or password_hash.startswith("$2a$")
    finally:
        db.close()


def test_patient_appointment_invoice_flow(client: TestClient, admin_token: str):
    headers = auth_headers(admin_token)

    patient = client.post(
        "/api/v1/patients",
        headers=headers,
        json={"first_name": "Ana", "last_name": "Torres", "dni": "12345678"},
    )
    assert patient.status_code == 201, patient.text
    patient_id = patient.json()["id"]
    assert patient.json()["file_number"].startswith("HC-")

    doctor = client.post(
        "/api/v1/users",
        headers=headers,
        json={
            "email": "dra@clinica.com",
            "password": "medico123",
            "full_name": "Dra. Ruiz",
            "role": "medico",
            "specialty": "Clinica general",
        },
    )
    assert doctor.status_code == 201, doctor.text
    doctor_id = doctor.json()["id"]

    first = client.post(
        "/api/v1/appointments",
        headers=headers,
        json={
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "start_time": "2026-11-02T09:00:00",
            "end_time": "2026-11-02T09:30:00",
            "reason": "Control",
        },
    )
    assert first.status_code == 201, first.text

    overlap = client.post(
        "/api/v1/appointments",
        headers=headers,
        json={
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "start_time": "2026-11-02T09:15:00",
            "end_time": "2026-11-02T09:45:00",
        },
    )
    assert overlap.status_code == 409

    doctor_login = client.post(
        "/api/v1/auth/login", json={"email": "dra@clinica.com", "password": "medico123"}
    )
    assert doctor_login.status_code == 200, doctor_login.text
    doctor_headers = auth_headers(doctor_login.json()["access_token"])

    record = client.post(
        f"/api/v1/patients/{patient_id}/records",
        headers=doctor_headers,
        json={"diagnosis": "Gripe", "treatment": "Reposo"},
    )
    assert record.status_code == 201, record.text

    admin_record = client.post(
        f"/api/v1/patients/{patient_id}/records",
        headers=headers,
        json={"diagnosis": "Intento administrativo"},
    )
    assert admin_record.status_code == 403

    invoice = client.post(
        "/api/v1/invoices",
        headers=headers,
        json={
            "patient_id": patient_id,
            "items": [
                {"concept": "Consulta", "quantity": 1, "unit_price": "50.00"},
                {"concept": "Análisis", "quantity": 2, "unit_price": "10.00"},
            ],
        },
    )
    assert invoice.status_code == 201, invoice.text
    assert invoice.json()["total"] in ("70.00", "70.0")
    invoice_id = invoice.json()["id"]

    paid = client.post(
        f"/api/v1/invoices/{invoice_id}/payments",
        headers=headers,
        json={"amount": "70.00", "method": "efectivo"},
    )
    assert paid.status_code == 200
    assert paid.json()["status"] == "pagada"

    item = client.post(
        "/api/v1/inventory",
        headers=headers,
        json={"sku": "MED-001", "name": "Paracetamol", "category": "medicamento", "stock": 10, "min_stock": 5},
    )
    assert item.status_code == 201, item.text
    item_id = item.json()["id"]

    salida = client.post(
        f"/api/v1/inventory/{item_id}/movements",
        headers=headers,
        json={"change": -12, "reason": "uso clínico"},
    )
    assert salida.status_code == 409

    entr = client.post(
        f"/api/v1/inventory/{item_id}/movements",
        headers=headers,
        json={"change": -3, "reason": "uso clínico"},
    )
    assert entr.status_code == 201
    assert entr.json()["stock_after"] == 7
