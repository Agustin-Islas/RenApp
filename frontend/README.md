# RenApp - Frontend Client

Aplicación cliente de **RenApp**, construida para facilitar el registro diario de diálisis peritoneal a los pacientes, y proveer un panel de monitoreo clínico avanzado para los nefrólogos.

El frontend está desarrollado con **Flutter**, permitiendo un despliegue nativo multiplataforma (Web, Android, iOS y Desktop) compartiendo una misma base de código.

## 🚀 Tecnologías Principales

- **Framework:** Flutter & Dart
- **Diseño UI:** Material Design 3 (Material UI)
- **Cliente HTTP:** Dio (con interceptores configurados para manejo de sesión)
- **Almacenamiento Local:** Flutter Secure Storage / Universal HTML
- **Generación de Reportes:** Paquetes `pdf` y `share_plus` para exportación de historias clínicas tabuladas.

## 🏗️ Estructura del Proyecto

La arquitectura del proyecto está orientada a "Features" (Funcionalidades) para garantizar la escalabilidad y mantenibilidad del código:

```text
lib/
├── core/             # Configuraciones globales, red (Dio), inyección de dependencias, temas visuales.
├── features/         # Módulos independientes por dominio:
│   ├── auth/         # Autenticación, registro y ruteo protegido (SessionGate).
│   ├── doctors/      # Panel clínico, gestión de pacientes asociados.
│   ├── patients/     # Perfil de paciente, historial clínico mensual.
│   ├── reports/      # Lógica de generación y exportación de PDFs médicos.
│   └── sessions/     # Formularios de recambios diarios, balances y ultrafiltración.
```

## 🔒 Flujo de Autenticación y Seguridad

- **Autenticación JWT:** La aplicación gestiona *Access Tokens* y *Refresh Tokens* de forma segura para las sesiones.
- **Interceptores Dinámicos:** Todas las peticiones al backend están protegidas. Si un token expira, la aplicación intenta refrescarlo silenciosamente sin interrumpir la experiencia del usuario.
- **Control de Acceso (SessionGate):** Un enrutador inteligente basado en roles que redirige automáticamente a los usuarios a sus interfaces correspondientes (Paciente o Doctor) dependiendo del contenido del token decodificado.

## ⚙️ Configuración y Ejecución Local

### Prerrequisitos
- Flutter SDK instalado en su canal estable.

### Configuración del Backend
Por defecto, la aplicación intentará conectarse a `http://localhost:8080`. Puedes sobrescribir la URL del backend al momento de compilar o correr la aplicación pasándole la variable `API_BASE_URL`.

### Ejecutar el Proyecto

```bash
cd frontend
flutter pub get

# Para correr en un servidor web local de pruebas:
flutter run -d web-server --web-port 3001 --dart-define=API_BASE_URL=http://localhost:8081
```

### Compilación para Producción

```bash
# Generar Android APK
flutter build apk

# Generar App Web de Producción
flutter build web --dart-define=API_BASE_URL=https://tu-backend-api.com
```

---
*Nota: Para detalles sobre pipelines de CI/CD en Codemagic o metodologías de despliegue, el equipo interno debe referirse a la documentación del Vault.*
