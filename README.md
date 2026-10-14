# Cupo - app móvil del parqueadero

App Android (Kotlin, Jetpack Compose, Material 3) para los usuarios de un parqueadero de 6 carros y 1 moto. Se conecta al backend Spring Boot del proyecto (`../backend`).

Video de la versión 1.0: https://youtu.be/dBP5H_squB4
Video de la versión 1.1: https://youtu.be/QO1wIvvKo7I

## Qué hace
- **Acceso:** sin autorregistro. El administrador crea al usuario y le entrega un código; con él se activa la cuenta, se crea un PIN de 6 dígitos y se acepta el aviso de privacidad. Después se entra con PIN o con huella. Al cerrar la app hay que volver a entrar.
- **Inicio:** estado de la mensualidad, placa y cupo; puerta tipo persiana (simulada) que se abre deslizando hacia arriba y se cierra sola.
- **Pagos:** próximo periodo y monto, subida del comprobante (cámara o galería, JPEG/PNG hasta 5 MB) e historial por año.
- **Actividad:** resumen (días restantes, pagos del año, visitas, permanencia, hora habitual), visitas por semana y movimientos.
- **Perfil:** datos y vehículo, cambio de PIN, huella, tema claro/oscuro/sistema y cierre de sesión.
- **Cámaras:** vista de demostración (no hay video real).

## Requisitos
- Android Studio con JDK 17 y SDK 35. El teléfono necesita Android 7.0 (API 24) o superior.
- El backend corriendo (`docker compose up` en `../backend`), que escucha en `127.0.0.1:8080`.

## Ejecutar
1. Abre la carpeta `mobile` en Android Studio y sincroniza Gradle.
2. **Emulador:** no hace falta configurar nada (usa `http://10.0.2.2:8080/`).
3. **Teléfono real por cable:** activa la depuración USB, añade en `local.properties`:
   ```
   cupo.apiUrl=http://127.0.0.1:8080/
   ```
   ejecuta `adb reverse tcp:8080 tcp:8080`, sincroniza Gradle y pulsa Run.
4. Para entrar necesitas un usuario creado desde el panel web del administrador y su código de activación.

## Tests
```
./gradlew :app:testDebugUnitTest
```
Pruebas unitarias en JVM de ViewModels y repositorios con una API falsa; no requieren emulador.

## Configuración del backend que afecta a la app
- Duración de la puerta abierta: propiedad `accesos.apertura-segundos` del backend (la app muestra la cuenta regresiva según `abiertaHasta`).

## Novedades de la versión 2.0 (rediseño)
- Nueva marca "Cupo": tema claro/oscuro, tipografía Space Grotesk + Inter y componentes propios (ver `docs/diseno/sistema-cupo.md`).
- Activación de cuenta en 3 pasos (celular y código, PIN, aviso de privacidad) y login con PIN o huella.
- Puerta tipo persiana con gesto, vibración y cuenta regresiva; cámaras con vista simulada.
- Pagos con comprobante validado (JPEG/PNG, hasta 5 MB) e historial por año.
- Actividad con indicadores y gráfico de visitas por semana (Vico).
- Seguridad: el token solo vive en memoria (al cerrar la app hay que volver a entrar), el PIN de la huella se guarda cifrado con el Keystore, HTTP solo en debug y capturas bloqueadas en release.
- Se corrigen los errores de la versión 1.0: `userId` ya no se envía en pagos, el login maneja errores y carga, sin pantallas duplicadas en la navegación y la puerta revierte si falla.
