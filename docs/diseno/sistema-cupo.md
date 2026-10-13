# Sistema de diseño Cupo

Estilo de señalética vial: azul de la señal P, amarillo de placa colombiana, neutros asfalto. Código en `ui/theme` (tokens) y `ui/components`. Tema claro/oscuro según el sistema o `ThemeMode` (Perfil). Sin color dinámico.

## Color (contraste WCAG; AA = 4.5 texto, 3.0 elementos gráficos)

| Rol | Claro | Oscuro | Par verificado (claro / oscuro) |
|---|---|---|---|
| surface / background | #F7F8FA | #101318 | - |
| onSurface | #14171C | #E2E5EA | sobre surface: 16.9 / 14.7 |
| onSurfaceVariant | #4A5361 | #C0C7D1 | sobre surface: 7.3 / 10.9 |
| primary | #0B5BD3 | #8AB4FF | sobre surface: 5.7 / 8.9 |
| onPrimary | #FFFFFF | #002F6C | sobre primary: 6.1 / 6.2 |
| primaryContainer / on | #D8E5FF / #00214F | #0A47A6 / #D8E5FF | 12.5 / 6.7 |
| outline | #737C89 | #8A93A0 | sobre surface: 3.9 / 6.0 |
| error | #BA1A1A | #FFB4AB | sobre surface: 6.1 / 11.0 |
| exito | #1B7F3B | #6FD48E | sobre surface: 4.8 / 10.2 |
| peligro | #C62828 | #FF8A80 | sobre surface: 5.3 / 8.2 |
| advertencia | #8A5300 | #FFC857 | sobre surface: 6.0 / 12.1 |
| neutro | #5B6472 | #AEB6C2 | sobre surface: 5.6 / 9.1 |
| exitoContainer / on | #D3F5DC / #00391A | #0E4F27 / #B7F2C8 | 11.1 / 7.6 |
| peligroContainer / on | #FFDAD6 / #5C0A0A | #7A1616 / #FFDAD6 | > 10 / 8.3 |
| advertenciaContainer / on | #FFE2A8 / #3D2600 | #5A3D00 / #FFE2A8 | > 10 / 7.9 |
| plate / onPlate | #FFC400 / #14171C | igual | 11.2 |

- El amarillo #FFC400 solo se usa como fondo con texto #14171C (blanco sobre amarillo da 1.6: prohibido). En M3 vive en `tertiaryContainer` (claro) y `tertiary` (oscuro, con `onTertiary` oscuro).
- Estados siempre con icono + texto (`StatusBadge`), nunca solo color: libre/ok verde (CheckCircle), ocupado/error rojo (Cancel), por vencer ámbar (Schedule), suspendido gris (PauseCircle), info azul (Info).
- Acceso: `MaterialTheme.cupo.exito`, `.peligroContainer`, `.plate`, etc.

## Tipografía

Space Grotesk (display/headline/title) por su aire de señalética; Inter (body/label) por legibilidad. Tamaños en `sp` (escalan con el sistema). Cifras tabulares con `TextStyle.tabular()` o `TabularNumbers`; los `display*` ya las traen.

| Estilo | Tamaño/línea | Peso | Uso |
|---|---|---|---|
| displaySmall..Large | 36/44 - 57/64 | SemiBold | cifras protagonistas |
| headlineLarge | 32/40 | Bold | valor de KpiCard |
| headlineMedium/Small | 28/36, 24/32 | Bold, SemiBold | título de pantalla |
| titleLarge/Medium/Small | 22/28, 16/24, 14/20 | SemiBold, SemiBold, Medium | tarjetas, secciones |
| bodyLarge/Medium/Small | 16/24, 14/20, 12/16 | Normal | texto |
| labelLarge/Medium/Small | 14/20, 12/16, 11/16 | SemiBold, SemiBold, Medium | botones, badges |

## Espaciado y forma

- `Spacing`: xxs 4, xs 8, sm 12, md 16, lg 24, xl 32, xxl 48 dp. Margen de pantalla `md`; entre tarjetas `sm`; entre secciones `lg`.
- `Radius`: sm 8 (controles, chips), md 12 (tarjetas), lg 20 (hojas, diálogos, badges). `CupoShapes` los mapea a M3. Excepción: `PlateChip` usa 4/6/8 dp proporcionales a la placa.
- Áreas táctiles mínimas 48 dp (`SectionHeader` reserva 48 dp de alto).

## Componentes (`ui/components`)

- `PlateChip(placa, size = Small|Medium|Large)`: formatea "ABC 123"; TalkBack lee "Placa A B C 1 2 3".
- `StatusBadge(tipo: StatusTipo, texto)`: icono + texto, un solo nodo de accesibilidad.
- `KpiCard(titulo, valor?, apoyo?, icono?)`: valor con unidad, apoyo con período; null -> "Sin datos todavía".
- `EmptyState`, `ErrorState` (mensaje en live region, botón "Reintentar"), `Skeleton` (brillo 1200 ms lineal, estático con reducir animaciones), `SectionHeader` (heading).
- `rememberReducedMotion()`: true si la escala de animación del sistema es 0.
- `@CupoPreviews` + `PreviewSurface {}` para previsualizar en claro y oscuro.

## Especificación Etapa 2: DoorControl (persiana)

**Anatomía** (ancho máx. 280 dp, centrado): marco 240 x 288 dp, radio `Radius.md`, trazo 2 dp `outline`, fondo `surfaceContainerLow`. Cajón superior 32 dp (`surfaceContainerHighest`). Hueco de 224 dp con 8 lamas de 28 dp (relleno `surfaceContainerHighest`, línea de luz 1 dp `outlineVariant` en el borde superior de cada una). Barra inferior 32 dp con manija de 56 x 8 dp, radio 4, relleno `plate`, borde 1 dp `plateBorder`. Detrás de la persiana: `primaryContainer` con icono de carro `onPrimaryContainer`. Etiqueta "Simulación" (labelSmall, borde `outlineVariant`, texto `onSurfaceVariant`) arriba a la derecha, fuera del marco. Debajo: texto de ayuda y botón alternativo `FilledTonalButton` "Abrir puerta" (48 dp).

**Gesto**: arrastre vertical hacia arriba; `progreso = desplazamiento / 224 dp` (0..1). Las lamas se recogen de abajo hacia arriba siguiendo el dedo (cada lama se comprime hacia el cajón cuando el borde inferior la alcanza). Háptica `TextHandleMove` cada vez que se recoge una lama; `LongPress` al cruzar el 50 %. Al soltar: si progreso >= 0.5 sube hasta 1 con resorte `dampingRatio 0.75, stiffness 400` y se envía la solicitud; si no, vuelve a 0 con `dampingRatio 0.6, stiffness 800`.

| Estado | Visual | Texto |
|---|---|---|
| Reposo | persiana abajo | "Desliza hacia arriba para abrir" |
| Arrastrando | lamas siguen el dedo | "Suelta para abrir" (desde 50 %) |
| Enviando | persiana arriba, indicador lineal `primary` en el cajón | "Abriendo..." |
| Abierta | arriba + tarjeta de evento + anillo con la duración de `abiertaHasta` (15 s por defecto) | "Puerta abierta" |
| Denegada | baja (resorte de retorno) + sacudida + motivo | "No pudimos abrir" + motivo |
| Error de red | baja + `ErrorState` compacto | "Sin conexión con el parqueadero." + "Reintentar" |

- **Abierta**: tarjeta con `StatusBadge` EXITO "ENTRADA" o INFO "SALIDA", `PlateChip` Small y hora "HH:mm" tabular ("Entrada registrada a las 7:42"). Anillo de 64 dp, trazo 6 dp, progreso `primary` sobre pista `outlineVariant`, número de segundos en titleMedium tabular. En 0 la persiana baja en 600 ms `FastOutSlowInEasing`. Arrastrar hacia abajo solo acelera el cierre visual y muestra un snackbar "Se cierra sola".
- **Sacudida**: desplazamiento horizontal 0, -8, 8, -6, 6, -3, 0 dp en 360 ms total; háptica `LongPress` al inicio.
- **Motivos** (bajo el título, bodyMedium, `StatusBadge` del tipo indicado):

| Código | Tipo | Texto |
|---|---|---|
| USUARIO_VENCIDO | ADVERTENCIA | "Tu mensualidad venció. Sube tu comprobante en Pagos para reactivar el acceso." |
| USUARIO_SUSPENDIDO | NEUTRO | "Tu cuenta está suspendida. Comunícate con la administración." |
| USUARIO_DE_BAJA | NEUTRO | "Tu cuenta fue dada de baja. Comunícate con la administración." |
| SIN_CUPO | PELIGRO | "No hay cupos libres en este momento. Intenta más tarde." |
| TIPO_CUPO_DISTINTO | PELIGRO | "No hay cupo libre para tu tipo de vehículo." |
| YA_DENTRO | INFO | "Tu vehículo ya figura dentro del parqueadero." |
| PLACA_DESCONOCIDA | PELIGRO | "Esta placa no está registrada en tu cuenta." |
| SALIDA_CON_DEUDA | ADVERTENCIA (permitida: estado Abierta) | "Salida registrada. Tienes un pago pendiente; súbelo en Pagos." |

- **TalkBack**: el marco es un nodo `Role.Button` con descripción "Puerta del parqueadero" y `stateDescription` "Cerrada" / "Abriendo" / "Abierta, se cierra en N segundos"; acción de clic "abrir". El resultado (éxito, motivo, error) va en una live region Polite. El anillo no anuncia cada segundo.
- **Reducir animaciones**: sin resortes ni sacudida; cambios de estado con fundido de 150 ms; la persiana salta a 0 o 1; el anillo se reemplaza por "Se cierra en N s". Háptica se mantiene.

## Especificación Etapa 2: Cámaras

- **Lista** (`LazyColumn`, padding `md`, separación `sm`): fila de 72 dp con icono Videocam en caja de 40 dp `primaryContainer`, nombre (titleMedium), ubicación (bodySmall) y `StatusBadge` EXITO "Activa" o NEUTRO "Sin señal" (icono VideocamOff). Toque abre la vista. Estados: `Skeleton` x3 filas, `EmptyState` "Sin cámaras" / "Aún no hay cámaras configuradas.", `ErrorState` con reintentar.
- **Vista de demostración**: marco 16:9, ancho completo, radio `Radius.md`. Fondo degradado lineal entre `#0B1A33` y `#14171C` cuyo ángulo rota 360 grados en 20 s; líneas de escaneo horizontales de 1 dp cada 4 dp al 6 % de blanco y una banda de 24 dp que baja en 4 s. Superior izquierda: punto 8 dp `peligro` que pulsa (opacidad 1 a 0.4, 1 s) + "EN VIVO" (labelMedium blanco) si activa; si no, centro con icono VideocamOff y "Sin señal" y sin animaciones. Superior derecha: reloj "HH:mm:ss" tabular blanco sobre fondo negro 50 %, se actualiza cada segundo. Inferior izquierda: etiqueta "Vista simulada" (labelSmall, mismo fondo). Todo texto sobre el video lleva ese fondo para asegurar contraste. Sin reproductor ni red.
- **Accesibilidad**: el marco es un nodo con descripción "Vista simulada de {nombre}, {en vivo | sin señal}"; el reloj no se anuncia. Con reducir animaciones el degradado y la banda quedan fijos y el punto no pulsa.
