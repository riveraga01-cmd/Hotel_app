# 🏨 Hotel Rivera — Aplicación Android

Sistema nativo de administración y control operativo para **Hotel Rivera**, desarrollado con Kotlin, Jetpack Compose y Firebase (Firestore en tiempo real y Firebase Authentication con Google Sign-In).

Compatible con teléfonos celulares en posición vertical y tablets en mostrador de recepción en orientación horizontal.

---

## 📥 Cómo Descargar el Archivo APK desde GitHub

Tienes tres formas sencillas de obtener el instalador `.apk` de la aplicación:

### Método 1: Descargar desde GitHub Releases (Recomendado)
1. En la página principal de este repositorio en GitHub, dirígete a la columna lateral derecha y haz clic en la sección **"Releases"**.
2. Selecciona la versión más reciente (ejemplo: `v1.0.0`).
3. En la sección **Assets** (Archivos adjuntos), haz clic sobre **`HotelRivera-v1.0.apk`** para iniciar la descarga directa en tu teléfono, tablet o computadora.

### Método 2: Descargar desde GitHub Actions (Compilaciones automáticas)
Cada vez que se suben cambios al repositorio, GitHub compila automáticamente el APK más reciente:
1. En la barra superior del repositorio en GitHub, haz clic en la pestaña **"Actions"**.
2. En la lista de flujos, haz clic sobre la ejecución más reciente con el nombre **"Build & Release Android APK"**.
3. Baja hasta la sección **"Artifacts"** al final de la página.
4. Haz clic en **`HotelRivera-Android-APK`**. Se descargará un archivo ZIP que contiene el APK listo para instalar (`HotelRivera-v1.0.apk`).

### Método 3: Descargar directamente desde Google AI Studio
1. En el entorno de AI Studio Build, haz clic en el menú de opciones o ajustes de la aplicación.
2. Selecciona la opción **Export / Download APK**.
3. El archivo APK se descargará inmediatamente a tu equipo.

---

## 📲 Cómo Instalar el APK en un Dispositivo Android

1. **Copiar o enviar el archivo `.apk` al dispositivo:**
   - Puedes enviarlo por WhatsApp, Telegram, correo electrónico (`riverahotel01@gmail.com`), Google Drive o cable USB.
2. **Habilitar fuentes desconocidas:**
   - Al pulsar sobre el archivo `.apk` descargado, si Android muestra el mensaje de seguridad *"Por motivos de seguridad, tu teléfono no tiene permiso para instalar aplicaciones desconocidas de esta fuente"*, pulsa en **Ajustes** y activa la casilla para esa aplicación (Chrome, Gestor de Archivos, etc.).
3. **Instalación:**
   - Pulsa **Instalar** y luego **Abrir**.
4. **Acceso del Personal:**
   - Inicia sesión con la cuenta de Google autorizada del hotel para sincronizar las habitaciones, reservaciones y cobros en tiempo real.

---

## 🛠️ Compilación Local con Gradle (Para Desarrolladores)

Si deseas compilar el APK manualmente en tu propia computadora:

```bash
# Compilar el APK de depuración
./gradlew :app:assembleDebug

# La ruta del archivo generado es:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛡️ Características Principales
- **Disponibilidad en Tiempo Real:** Sincronización instantánea de estados de habitaciones (`DISPONIBLE`, `OCUPADA`, `RESERVADA`, `DESHABILITADA`) mediante Cloud Firestore.
- **Autenticación Segura:** Inicio de sesión exclusivo para personal mediante Firebase Auth y Google Sign-In.
- **Módulo de Reservaciones:** Calendario interactivo con vistas de Día, Semana y Mes, selectores de fecha y hora exactos, y prevención automática de cruces de horario.
- **Facturación y Control de Estadías:** Generación de facturas, cobro de tarifas por horas y noche completa, registro de consumos y cobros extra.
- **Diseño Adaptativo:** Cuadrículas y paneles optimizados para teléfonos celulares y tablets de mostrador.
