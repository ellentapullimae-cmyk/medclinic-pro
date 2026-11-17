# ADR-002: Backend como monolito modular en FastAPI

## Estado
Aceptado

## Contexto
La clínica tiene 6 módulos (auth, pacientes, citas, historia clínica, facturación, inventario). El equipo es pequeño y el volumen esperado es bajo (una clínica, decenas de usuarios). Se necesita rapidez de desarrollo y despliegue sencillo.

## Decisión
Un **monolito FastAPI** organizado por dominio: `api/routers/` (HTTP), `services/` (reglas de negocio), `models/` (SQLAlchemy), `schemas/` (Pydantic). Cada módulo es un paquete aislado; la única dependencia compartida son `core/`, `db/` y `models/`.

## Consecuencias

### Positivas
- Un solo proceso que desplegar, un solo esquema de BD transaccional.
- Reglas de negocio testables sin HTTP (services puros con sesión inyectada).
- Extracción futura de un módulo (p. ej. facturación) a servicio propio si hace falta.

### Negativo
- Despliegue conjunto: un bug puede afectar todo el proceso.
- Escalado vertical antes que horizontal.

### Neutral
- Sin eventos asíncronos; las notificaciones se añadirán después si se necesitan.

## Alternativas consideradas
- **Microservicios**: descartado — complejidad operativa (red, consistencia distribuida, deployments) injustificada para una clínica.
- **Django + DRF**: descartado — FastAPI elegido por el usuario; además, tipado Pydantic y OpenAPI automático encajan mejor con el cliente Android.
- **FastAPI sin capa services** (lógica en routers): descartado — mezclaría HTTP con reglas de negocio y dificultaría tests.

## Referencias
- Plantilla de arquitectura: `docs/arquitectura.md`.
