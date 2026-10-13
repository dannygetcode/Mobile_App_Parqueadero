# App móvil — Parqueadero (Android)

App de los usuarios del parqueadero: registro con verificación por código y PIN, login, abrir la puerta, subir comprobantes de pago y ver cámaras. Habla con el backend (`backend`, Spring Boot).

## Stack
- Kotlin 2.0.0, Android Gradle Plugin 8.8.0, Jetpack Compose (BOM 2024.04.01) + Material 3
- Navigation Compose 2.7.3, Retrofit 2.9.0 + Gson, Coil, coroutines
- `minSdk 24`, `compileSdk/targetSdk 35`, paquete `com.parqueadero.appparqueadero`

## Comandos
- Compilar: `./gradlew assembleDebug` · Tests: `./gradlew test`
- Para correr en dispositivo o emulador usa Android Studio. Al terminar: `./gradlew --stop` (los daemons de Gradle consumen mucha RAM).

## Estructura (`app/src/main/java/com/parqueadero/appparqueadero/`)
- `ui/NavGraph.kt` — todas las rutas: `welcome`, `login` y `login/{telefono}`, `register`, `verification/{telefono}`, `createPin/{telefono}/{codigo}`, `home`, `pagos`, `camaras`
- `ui/<pantalla>/` — `login`, `register`, `main` (home), `payments`, `camaras`, `components`, `theme`
- `data/model/` — DTOs (deben coincidir con los del backend) · `data/network/` — `ApiService` y `RetrofitClient`
- `utils/Constants.kt` — `BASE_URL`: emulador `10.0.2.2:8080`, dispositivo real una IP de la red local

## Reglas para trabajar aquí
- Los DTOs de `data/model` espejan los del backend (`backend/.../dto`). Si cambias uno, revisa el otro lado.
- Las IPs de la red local también están en `res/xml/network_security_config.xml` (tráfico HTTP permitido). Si cambia la IP, hay que actualizar `Constants.kt` y ese archivo.
- No edites `gradle.properties` del repo: el límite de memoria de Gradle se configura en `~/.gradle/gradle.properties`.
- No subas cambios de `.idea/` que no sean intencionales.

## Deuda conocida (no la "arregles" sin que se pida)
- IP de la LAN fija en el código; sin variantes de build (debug/release) para el servidor.
- `RetrofitClient` no tiene interceptor de autenticación.
- Solo hay tests de ejemplo.

<!-- code-review-graph MCP tools -->
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
