# Sistema de Gestión de Clínica

Arquitectura completa: **app Android (Kotlin + Jetpack Compose)** + **API REST (FastAPI)**.

## Estructura del proyecto

```
clinica/
├── docs/                  Documentación de arquitectura
│   ├── arquitectura.md    Diagramas, módulos, NFR, seguridad
│   ├── modelo-datos.md    Modelo entidad-relación
│   ├── api.md             Contrato REST (endpoints)
│   └── adr/               Architecture Decision Records
├── backend/               API FastAPI (Python 3.12)
└── android/               Proyecto Android Studio (Kotlin + Compose)
```

## Módulos

| Módulo | Backend | Android |
|--------|---------|---------|
| Usuarios / Roles (JWT) | `/api/v1/auth`, `/api/v1/users` | `presentation/auth` |
| Pacientes | `/api/v1/patients` | `presentation/patients` |
| Citas / Agenda | `/api/v1/appointments` | `presentation/appointments` |
| Historia clínica | `/api/v1/clinical-records` | `presentation/clinical` |
| Facturación | `/api/v1/invoices`, `/payments` | `presentation/billing` |
| Inventario | `/api/v1/inventory` | `presentation/inventory` |

Roles: `admin`, `medico`, `recepcion`.

## Ejecutar el backend

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate          # Windows
pip install -r requirements.txt
uvicorn app.main:app --reload   # http://127.0.0.1:8000/docs
```

Usuario inicial: `admin@clinica.com` / `admin123` (sembrado automáticamente).

## Ejecutar el Android

1. Abrir la carpeta `android/` en Android Studio.
2. Sincronizar Gradle (Android Studio ofrece crear el wrapper si no existe).
3. Cambiar `BASE_URL` en `core/network/ApiClient.kt` si la API no corre en el emulador en `10.0.2.2:8000`.
4. Run en emulador o dispositivo.

### Capas Android (MVVM + Clean)

```
app/src/main/java/com/clinica/
├── core/        Red (Retrofit), sesión (EncryptedSharedPreferences), tema
├── domain/      Modelos (kotlinx.serialization) + interfaces de repositorio
├── data/        APIs Retrofit + implementaciones + módulos Hilt
└── presentation Pantallas Compose + ViewModels (StateFlow) + navegación
```

Login de prueba: `admin@clinica.com` / `admin123`

## Comandos útiles

```bash
# Tests backend
cd backend && pytest

# Stub del modelo de datos (SQLite)
# Se crea automáticamente en backend/clinica.db al arrancar
```

## Orden recomendado de implementación

1. Backend: auth + pacientes → citas → historia clínica → facturación → inventario.
2. Android: login + dashboard → pacientes → citas → resto de módulos.
3. Migraciones con Alembic y tests E2E cuando el MVP esté estable.
