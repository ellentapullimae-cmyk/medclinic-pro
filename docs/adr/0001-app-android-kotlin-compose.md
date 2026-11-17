# ADR-001: App Android con Kotlin + Jetpack Compose (MVVM/Clean)

## Estado
Aceptado

## Contexto
Se necesita una app para que recepción, médicos y administración operen la clínica desde tablets/teléfonos. Debe mantenerse con un equipo pequeño y evitar pantallas duplicadas por plataforma. El usuario trabaja en Android Studio.

## Decisión
Android nativo con **Kotlin**, **Jetpack Compose** (declarativo), arquitectura **MVVM + Clean Architecture** en 3 capas (`presentation` → `domain` → `data`), **Hilt** (DI), **Retrofit** + kotlinx.serialization (red) y **StateFlow** para estado de UI.

## Consecuencias

### Positivas
- UI declarativa: menos código, pantallas fáciles de iterar.
- Clean Architecture: se puede testear la lógica sin emulador.
- Hilt + Retrofit: patrón estándar, fácil de encontrar ayuda.

### Negativo
- Curva inicial de aprendizaje de Compose/Hilt si el equipo viene de XML.
- Sin cache offline inicialmente (Room queda para fase 2).

### Neutral
- El ViewModel expone `StateFlow<UiState>`, patrón repetido en todos los módulos.

## Alternativas consideradas
- **Flutter**: descartado — se pidió Android Studio / nativo.
- **React Native**: descartado — mismo motivo, y menos control sobre UI nativa.
- **Vista XML + ViewModel clásico**: descartado — más verboso, sin ventaja para el equipo.

## Referencias
- Documentación: `docs/arquitectura.md` sección 4.
