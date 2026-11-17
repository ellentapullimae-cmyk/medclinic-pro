# ADR-003: Base de datos relacional — SQLite (dev) → PostgreSQL (prod)

## Estado
Aceptado

## Contexto
Los datos son fuertemente relacionales (paciente–citas–facturas–stock) y requieren integridad transaccional (totales de factura, stock, unicidad de números). Se debe poder desarrollar sin levantar servidores, pero con ruta de migración clara.

## Decisión
**SQLAlchemy 2.0 + SQLite** como BD por defecto en desarrollo (archivo `clinica.db`), con la configuración apuntando a **PostgreSQL** vía `DATABASE_URL` en producción. ORM único para ambos; migraciones con Alembic en la fase productiva.

## Consecuencias

### Positivas
- Cero configuración para empezar: `uvicorn app.main:app` crea el esquema y un admin.
- Mismo ORM y queries en dev y prod.
- Postgres aporta ACID, copias de seguridad y cifrado en reposo cuando se necesite.

### Negativo
- SQLite no soporta escrituras concurrentes intensivas ni `JSONB`.
- Cambiar de motor implica revisar tipos (fechas, booleanos, JSON) al migrar.

### Neutral
- El cambio a Postgres es solo cambiar `DATABASE_URL`; ninguna lógica depende del motor.

## Alternativas consideradas
- **SQLite desde inicio en producción**: descartado — riesgo de pérdida de datos y sin replicación.
- **PostgreSQL desde el día 1**: descartado por ahora — coste operacional innecesario en dev; se reevaluará al salir a producción.
- **MongoDB**: descartado — el modelo es relacional con muchas integridades.
- **Firebase/Room como fuente de verdad**: descartado — el backend debe ser la única fuente de verdad y quedar consultable desde web futuro.

## Referencias
- `docs/modelo-datos.md` — diagrama ER.
