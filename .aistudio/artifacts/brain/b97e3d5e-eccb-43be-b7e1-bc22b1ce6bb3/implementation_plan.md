# Compatibilidad con Android y Diseño Adaptativo Multi-Dispositivo (Hotel Rivera)

Este plan establece las optimizaciones necesarias para garantizar que la aplicación Hotel Rivera se instale y ejecute de manera óptima en teléfonos y tablets Android mediante archivo APK, adaptando su interfaz fluida y automáticamente a celulares en posición vertical y tablets en orientación horizontal de recepción.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> 1. **Objetivo de Compatibilidad:** Instalación directa del archivo APK en teléfonos y tablets Android (compatibilidad desde Android 7.0 / API 24 hasta Android 15 / API 35+).
> 2. **Orientación de Dispositivos:** Soporte nativo y responsivo para celulares en formato vertical (portrait) y tablets en formato horizontal (landscape / mostrador de recepción).

- **Estrategia de Pantallas Adaptativas:** Se implementará detección de ancho de pantalla (`BoxWithConstraints` y `WindowInsets`) para alternar automáticamente entre navegación compacta inferior (celulares) y barra de navegación lateral / cuadrícula multi-columna (tablets en horizontal).
- **Generación y Despliegue de APK:** Se asegurará que la configuración del manifest, firmas de depuración y empaquetado permitan la instalación fluida del archivo APK sin bloqueos de arquitectura ni permisos incompatibles.

---

## 1. Overview & Core Concept

- **Qué hace:** Proporciona una experiencia de usuario robusta y nativa en cualquier dispositivo Android, permitiendo que el personal de recepción use tablets fijas en mostrador con vista panorámica (cuadrícula amplia de habitaciones, panel lateral de detalles de estadías y reservaciones) y que el gerente o personal móvil opere desde teléfonos celulares en vertical con listas optimizadas y navegación con una mano.
- **Público Objetivo:** Recepcionistas, administradores y gerente general de Hotel Rivera en turnos diurnos y nocturnos.
- **Valor Principal:** Elimina restricciones de dispositivo: la misma aplicación se instala fácilmente mediante APK y se adapta con ergonomía profesional tanto a un smartphone estándar como a una tablet de 10 o 12 pulgadas en recepción.

---

## 2. User Experience & Visual Design

### Adaptación según Dispositivo y Orientación

| Característica | Teléfono (Vertical / Portrait) | Tablet (Horizontal / Landscape) |
| :--- | :--- | :--- |
| **Navegación Recepción** | Pestañas superiores / barra inferior compacta | Menú lateral ergonómico (*Navigation Rail*) |
| **Mosaico de Habitaciones** | 2 columnas compactas con scroll vertical fluido | 4 a 6 columnas panorámicas (*Grid adaptativo*) |
| **Detalle y Cobro de Estadía** | Cuadro de diálogo modal vertical con scroll | Diálogo centrado amplio con panel de 2 columnas (detalles + cobro) |
| **Calendario de Reservaciones** | Vista diaria y mensual con lista vertical apilada | Vista de mes con panel lateral derecho de reservaciones del día |
| **Gestión Gerente / Finanzas** | Tarjetas de métricas apiladas verticalmente | Cuadrícula de 3 columnas para ingresos, ocupación y gastos |

### Identidad Visual y Paleta
- Se mantiene el tema claro de alto contraste:
  - Fondo general: `#FFFFFF`
  - Encabezados y barras principales: Azul Marino `#0B2239` con texto blanco
  - Tarjetas y contenedores: Fondo blanco con bordes visibles `#E0E0E0`
  - Texto principal: `#111111` y secundario `#424242`
  - Botones de confirmación y liberación: Verde oscuro `#1B5E20`
  - Acentos de marca: Dorado `#D4AF37`
  - Estados de habitación: Verde (Disponible), Rojo (Ocupada), Ámbar (Reservada), Gris (Deshabilitada).

---

## 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Layouts Adaptativos con `BoxWithConstraints` en Compose**
  - *Enfoque Elegido:* Evaluar el ancho disponible (`maxWidth >= 600.dp` o `840.dp`) directamente en el nivel de pantalla de Recepción y Gerente.
  - *Por qué:* Permite responder tanto a tablets físicas como a rotación de pantalla en celulares grandes sin recargar el estado ni perder datos en formularios abiertos.
  - *Alternativa descartada:* Fijar la orientación a vertical mediante el `AndroidManifest.xml` (hubiera impedido aprovechar las pantallas horizontales de tablets de recepción).

- **Decisión 2: Compatibilidad de Instalación APK (Android 7.0 a Android 15+)**
  - *Enfoque Elegido:* Mantener `minSdk = 24` con desugaring de Java 8/11 y comprobaciones dinámicas de permisos en tiempo de ejecución (ej. `POST_NOTIFICATIONS` solo en Android 13+ y cámara condicional).
  - *Por qué:* Asegura que el archivo APK se instale sin errores de análisis sintáctico (*Parse Error*) en cualquier teléfono o tablet económica del hotel.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HOTEL RIVERA ANDROID APP                        │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
               ┌───────────────────┴───────────────────┐
               ▼                                       ▼
    ┌──────────────────────┐               ┌──────────────────────┐
    │  CELULAR (PORTRAIT)  │               │  TABLET (LANDSCAPE)  │
    │  maxWidth < 600.dp   │               │  maxWidth >= 600.dp  │
    ├──────────────────────┤               ├──────────────────────┤
    │ Top/Bottom Nav       │               │ Side Navigation Rail │
    │ 2-Col Room Grid      │               │ 4 to 6 Col Room Grid │
    │ Stacked Dialogs      │               │ Two-Pane Split View  │
    │ Single-Col Calendar  │               │ Month Grid + Day Pane│
    └──────────┬───────────┘               └───────────┬──────────┘
               │                                       │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  STATE & ARCHITECTURE (MVVM + COMPOSE)                 │
├────────────────────────────────────────────────────────────────────────┤
│ • HotelViewModel (StateFlows con soporte para retención de estado)     │
│ • LocalStore (Cache persistente SQLite / SharedPreferences local)      │
│ • Cloud Firestore (Sincronización multi-dispositivo en tiempo real)    │
└────────────────────────────────────────────────────────────────────────┘
```

### Plan de Cambios Específicos:
1. **Configuración de AndroidManifest y Build:**
   - Asegurar que `MainActivity` permita rotación fluida (`android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"`).
   - Validar que las configuraciones de firma y empaquetado del APK de AI Studio sean compatibles para instalación directa.
2. **Recepción en Modo Tablet / Horizontal (`RecepcionScreen.kt`):**
   - Incorporar `BoxWithConstraints`: cuando el ancho sea mayor o igual a `720.dp`, mostrar la barra de navegación en formato lateral izquierdo (*NavigationRail*) y distribuir el mosaico de habitaciones en cuadrícula expandida (4 o más columnas).
   - En la sección de reservaciones en tablet, mostrar el calendario a la izquierda y el listado de reservaciones del día seleccionado a la derecha en dos paneles simultáneos (*Two-Pane Layout*).
3. **Formularios y Diálogos:**
   - Asegurar que los diálogos de Nueva Reservación, Registro de Estadía y Factura utilicen `Modifier.widthIn(max = 640.dp)` con scroll vertical, para que nunca se desborden ni se vean estirados en tablets horizontales.
4. **Verificación de Compilación y APK:**
   - Ejecutar `compile_applet` para garantizar que la compilación sea exitosa y esté lista para exportación.
