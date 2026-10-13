# App móvil — Cupo (Android)

App de los usuarios del parqueadero (6 carros + 1 moto): activar la cuenta con el código del admin, entrar con PIN o huella, abrir la puerta (simulada), subir comprobantes de pago, ver su actividad y cámaras (simuladas). Habla con el backend (`../backend`, Spring Boot), que no se edita desde aquí. Contrato: `../backend/docs/contrato-movil.md`.

## Stack
- Kotlin 2.0.0, AGP 8.8.0, Jetpack Compose (BOM 2024.09.03; Vico sube Compose a 1.7.6) + Material 3. Versiones en `gradle/libs.versions.toml`.
- Navigation Compose 2.8.4, lifecycle 2.9.1, Retrofit 2.11 + Gson, OkHttp 4.12, DataStore, Coil 2.7, Vico 2.0.0 (gráficos), androidx.biometric 1.1.0.
- `minSdk 24` (java.time con desugaring), `compileSdk/targetSdk 35`, paquete `com.parqueadero.appparqueadero`.

## Comandos
- Compilar: `./gradlew assembleDebug` · Tests unitarios (JVM, sin emulador): `./gradlew :app:testDebugUnitTest`
- Desde Git Bash sin `local.properties`: `export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"` antes de gradlew.
- Para correr en dispositivo usa Android Studio. Al terminar: `./gradlew --stop` (los daemons de Gradle consumen mucha RAM).

## Servidor (BuildConfig.API_URL)
- Debug: `http://10.0.2.2:8080/` (emulador). Teléfono real: en `local.properties` (no versionado) `cupo.apiUrl=http://127.0.0.1:8080/` y `adb reverse tcp:8080 tcp:8080`.
- Release: `cupo.apiUrl.release=https://...` (solo HTTPS). HTTP solo en debug: `src/debug/res/xml/network_security_config.xml` (10.0.2.2, 127.0.0.1, localhost). Sin IPs reales en el código.

## Estructura (`app/src/main/java/com/parqueadero/appparqueadero/`)
- `AppContainer.kt` (dependencias sin Hilt), `CupoApp.kt`, `MainActivity.kt` (FragmentActivity por BiometricPrompt).
- `data/model` — DTOs del contrato (deben coincidir con `backend/.../dto`) · `data/network` — `ApiService`, `AuthInterceptor` (Bearer; 401 cierra sesión salvo `PUT /usuarios/yo/pin`), `Resultado`/`AppError`/`ErrorParser` (ProblemDetail).
- `data/session` — `SessionManager` (token solo en memoria), `BovedaPin` (PIN cifrado con clave del Keystore protegida por huella), `Preferencias` (tema, último teléfono).
- `data/repository` — Auth, Usuario, Pago, Acceso (puerta 403 = `PuertaDTO`, no ProblemDetail), `FuenteImagen` (lee y reduce el comprobante).
- `ui/navigation/CupoNavegacion.kt` — la sesión decide el grafo: acceso (`bienvenida`, `activar`, `login`) o principal con barra inferior (`inicio`, `pagos`, `actividad`, `perfil`) + `camaras`.
- `ui/<pantalla>/` — `auth` (bienvenida, activar, login), `inicio` (cuenta + `DoorControl`), `pagos`, `actividad`, `perfil`, `camaras`, `biometria`, `common` (`UiState`), `components`, `theme`.
- Diseño: `docs/diseno/sistema-cupo.md` (tokens, componentes, especificación de puerta y cámaras).

## Reglas para trabajar aquí
- Un ViewModel por pantalla con `StateFlow` y `UiState` (Loading/Empty/Error/Success); la lógica va en ViewModels/repositorios, que se prueban con `FakeApiService` (`app/src/test/.../support/TestSupport.kt`).
- Los DTOs de `data/model` espejan los del backend. Si cambias uno, revisa el otro lado. La app nunca envía `usuarioId`.
- Sesión: el token no se persiste. Cerrar la app (o un 401) obliga a entrar de nuevo con PIN o huella. No vuelvas a guardar el token en disco.
- Huella: se guarda el PIN (no el token), cifrado con AES-GCM y clave del Keystore con autenticación biométrica fuerte; se borra al cambiar el PIN, al activar con código nuevo, si el login con huella da 401 o si cambian las huellas del teléfono.
- Puerta: la duración la da el backend (`abiertaHasta`, propiedad `accesos.apertura-segundos`); la app usa 15 s si el valor no es razonable.
- Español sin emojis en UI, código y commits. No edites `gradle.properties` del repo (memoria de Gradle en `~/.gradle/gradle.properties`). No subas cambios de `.idea/` que no sean intencionales.

## Deuda conocida
- Sin tests instrumentados ni de UI (solo JVM).
- Si el sistema mata el proceso mientras la cámara toma la foto del comprobante, al volver hay que entrar de nuevo y la foto se pierde.

## MCP Tools: code-review-graph

**This project has a knowledge graph. Start with the code-review-graph
MCP tools to narrow scope, then read the source.** The graph is cheaper than scanning files and
gives you structural context (callers, dependents, test coverage) that file search cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

### Verify in the source

- Narrow scope with the graph, then read the source. Do not change code from graph output alone.
- For any non-trivial change, read the implementation and the relevant tests before concluding.
- Verify the exact source when touching behavior, database logic, migrations, retries, fallbacks,
  recovery, or compatibility code.
- When the graph and the source disagree, the source wins. The graph may be stale or may not
  model that relationship.
- An empty graph result can mean "not indexed" or "not statically visible", not "does not exist".

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes_tool` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context_tool` | Need source snippets for review — token-efficient |
| `get_impact_radius_tool` | Understanding blast radius of a change |
| `get_affected_flows_tool` | Finding which execution paths are impacted |
| `query_graph_tool` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool` | Finding functions/classes by name or keyword |
| `get_architecture_overview_tool` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.
<!-- /code-review-graph MCP tools -->
