# ADR-004: Autenticación con JWT y control de roles

## Estado
Aceptado

## Contexto
Tres perfiles usan la app: `admin` (todo), `medico` (fichas + historia clínica), `recepcion` (pacientes, citas, cobros). La app Android necesita llamadas stateless sin sesiones server-side.

## Decisión
- Login `POST /auth/login` → **JWT HS256** (expiración 60 min) con `sub`, `role`.
- Passwords con **bcrypt** (passlib).
- Autorización en FastAPI con dependencia `require_role(...)` por endpoint.
- Android guarda el token en **EncryptedSharedPreferences** y lo envía con un interceptor de OkHttp; ante `401` se fuerza re-login.

## Consecuencias

### Positivas
- Stateless: cualquier réplica de la API valida el token.
- Permisos declarados por endpoint, visibles en el código.
- Sin tablas de sesión que limpiar.

### Negativo
- Sin revocación inmediata del token (ventana de 60 min); se mitiga rotando `SECRET_KEY` si se compromete.
- El refresh está pendiente (fase 2): por ahora re-login al expirar.

### Neutral
- El rol viaja en el token; cambiar un rol implica volver a loguearse.

## Alternativas consideradas
- **Cookies + sesiones server-side**: descartado — más estado que mantener y peor en móvil.
- **OAuth2/OIDC con proveedor externo (Auth0)**: descartado — coste y dependencia innecesarios para uso interno; se puede añadir después.
- **API keys fijas por dispositivo**: descartado — sin granularidad por usuario/rol.

## Referencias
- `docs/api.md` — tabla de roles por endpoint.
