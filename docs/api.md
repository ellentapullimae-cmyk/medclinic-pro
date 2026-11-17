# Contrato API REST (`/api/v1`)

Autenticación: `Authorization: Bearer <token>` salvo en login/registro de acceso.
Formato: JSON. Errores: `{"detail": "..."}` con código HTTP estándar.

## Auth y usuarios

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/auth/login` | público | Devuelve `access_token` (JWT) |
| GET | `/users/me` | autenticado | Perfil del token |
| GET | `/users/doctors` | autenticado | Médicos activos (para agendar citas) |
| GET | `/users` | admin | Lista usuarios |
| POST | `/users` | admin | Crea usuario |
| PATCH | `/users/{id}` | admin | Activa/desactiva, cambia rol |

## Pacientes

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/patients?q=&page=` | todos | Lista/búsqueda por nombre o DNI |
| POST | `/patients` | admin, recepcion | Crea ficha (asigna `file_number`) |
| GET | `/patients/{id}` | todos | Ficha completa |
| PATCH | `/patients/{id}` | admin, recepcion | Actualiza |
| DELETE | `/patients/{id}` | admin | Baja lógica |

## Citas

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/appointments?date=&doctor_id=&status=` | todos | Agenda filtrada |
| POST | `/appointments` | admin, recepcion | Crea cita (409 si hay solape) |
| PATCH | `/appointments/{id}` | todos | Cambia estado/detalle |
| DELETE | `/appointments/{id}` | admin, recepcion | Cancela |

## Historia clínica

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/patients/{id}/records` | medico, admin | Registros del paciente |
| POST | `/patients/{id}/records` | medico | Crea entrada (firma con el token) |
| GET | `/clinical-records/{id}` | medico, admin | Detalle |

## Facturación

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/invoices?status=&patient_id=` | todos | Lista |
| POST | `/invoices` | admin, recepcion | Crea con ítems (total calculado server-side) |
| GET | `/invoices/{id}` | todos | Detalle con ítems y pagos |
| POST | `/invoices/{id}/payments` | admin, recepcion | Registra pago (cierra si cubre el total) |
| POST | `/invoices/{id}/cancel` | admin | Anula sin pagos |

## Inventario

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/inventory?q=` | todos | Ítems con stock |
| POST | `/inventory` | admin | Crea ítem |
| PATCH | `/inventory/{id}` | admin | Actualiza (ej. `min_stock`) |
| POST | `/inventory/{id}/movements` | admin, recepcion, medico | Entrada/salida (409 si stock insuficiente) |
| GET | `/inventory/{id}/movements` | admin | Historial |

## Operaciones transversales

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/health` | Liveness para monitoreo |

## Códigos usados

`200` OK · `201` creado · `400` validación · `401` sin token · `403` rol insuficiente · `404` no existe · `409` conflicto (solape/stock) · `422` esquema inválido · `500` error interno.
