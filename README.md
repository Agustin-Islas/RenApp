# RenApp

Sistema para registrar, consultar y acompanar el tratamiento diario de dialisis peritoneal. El proyecto combina un backend Spring Boot con una app Flutter multiplataforma para pacientes y doctores.

## Mision

RenApp busca convertir el registro diario de dialisis peritoneal en una experiencia simple, trazable y clinicamente util. La meta es que el paciente pueda cargar sus cambios sin friccion y que el doctor tenga informacion ordenada para detectar atrasos, tendencias de ultrafiltracion, balances anormales y necesidades de seguimiento.

El proyecto no reemplaza el criterio medico: organiza datos, mejora la continuidad del seguimiento y prepara la base para alertas, reportes y paneles clinicos.

## Estructura

```text
.
+-- backend-dialysis-record/
|   +-- README.md
|   +-- backend-dialysis-record/   # Spring Boot API
+-- frontend_dialysis_record/
|   +-- README.md
|   +-- lib/                       # Flutter app
+-- scripts/                       # scripts auxiliares
+-- INFORME_*.md                   # informes previos
+-- PLAN_*.md                      # planes previos de implementacion
```

## Estado actual

### Backend

- API REST con Spring Boot, JPA/Hibernate y PostgreSQL.
- Autenticacion JWT con access token y refresh token.
- Registro y login de pacientes y doctores.
- Roles `PATIENT`, `DOCTOR` y soporte parcial para `ADMIN` en endpoints.
- Endpoints de perfil `/api/patients/me` y `/api/doctors/me`.
- Gestion de pacientes asociados al doctor.
- Sesiones/cambios de dialisis por dia, rango y mes.
- Resumen de sesiones por dia y por mes.
- Soft delete para pacientes y sesiones mediante `active`.
- Validacion de concentraciones fijas y personalizadas por paciente.

### Frontend

- App Flutter orientada a Web, Android y escritorio.
- Login y registro de paciente/doctor.
- Persistencia de sesion con access/refresh token.
- Interceptor HTTP con refresh automatico.
- Home de paciente con registro diario de cambios.
- Historial mensual del paciente.
- Perfil editable del paciente, incluyendo concentraciones personalizadas.
- Home de doctor con lista de pacientes asociados.
- Selector para asociar pacientes existentes al doctor.
- Vista de detalle del paciente para doctor.
- Resumen mensual con ultrafiltrados por semana.
- Exportacion PDF mensual.
- Layouts ajustados para mobile y pantallas grandes.

## Como ejecutar

### Backend

Requisitos:

- Java 21.
- PostgreSQL local.
- Base `dialysis_db`, usuario `dialysis_user`, password `dialysis_pass`, o ajustar `application.properties`.

Ruta:

```powershell
cd "backend-dialysis-record\backend-dialysis-record"
```

Comando esperado:

```powershell
.\mvnw.cmd spring-boot:run
```

Nota de auditoria: en esta maquina el wrapper Maven falla antes de iniciar Maven con `Cannot start maven from wrapper`. Si ocurre, revisar `mvnw.cmd`, instalar Maven globalmente o regenerar el wrapper.

### Frontend

Ruta:

```powershell
cd frontend_dialysis_record
```

Web local:

```powershell
flutter run -d web-server --web-hostname 127.0.0.1 --web-port 3001
```

Build web:

```powershell
flutter build web
```

Configurar API:

```powershell
flutter run -d web-server --dart-define=API_BASE_URL=http://localhost:8080
```

## Auditoria tecnica

### Hallazgos de prioridad alta

1. `logout/all` no funciona correctamente con la configuracion actual.
   - `JwtAuthFilter.shouldNotFilter` excluye todas las rutas `/auth/`.
   - `SecurityConfig` permite `/auth/**` antes de declarar `/auth/logout/all` como autenticado.
   - Resultado probable: `logout/all` no recibe `Authentication`.
   - Recomendacion: no excluir `/auth/logout/all` del filtro JWT y mover su regla antes de `/auth/**`.

2. Asociacion doctor-paciente sin consentimiento ni aprobacion.
   - Cualquier doctor autenticado puede listar todos los pacientes y asociarse uno.
   - Esto cumple el flujo pedido, pero tiene riesgo de privacidad.
   - Recomendacion: agregar invitaciones, aprobacion por paciente/admin o una lista de pacientes disponibles con datos minimos.

3. Datos sensibles en configuracion plana.
   - `application.properties` contiene credenciales y secreto JWT.
   - Recomendacion: mover a variables de entorno, perfiles (`dev`, `prod`) y secretos de deploy.

4. `spring.jpa.hibernate.ddl-auto=update`.
   - Practico para desarrollo, riesgoso para produccion.
   - Recomendacion: Flyway/Liquibase para migraciones versionadas.

5. Validar convencion clinica de ultrafiltracion/balance.
   - Backend calcula `partial = infusion - drainage`.
   - En dialisis peritoneal muchas veces se interpreta UF como `drainage - infusion`.
   - Recomendacion: confirmar convencion con el usuario medico y renombrar variables si hace falta para evitar signos invertidos.

### Hallazgos de prioridad media

1. No hay tests automatizados suficientes.
   - Backend solo tiene test base de arranque.
   - Frontend no tiene tests de widgets ni flujos de auth/sesiones.
   - Recomendacion: pruebas de auth, autorizacion por rol, resumen mensual, CRUD de sesiones y UI critica.

2. El Maven wrapper esta roto en Windows en esta maquina.
   - Bloquea verificacion backend local.
   - Recomendacion: regenerar wrapper (`mvn -N wrapper:wrapper`) o revisar script/line endings.

3. `pom.xml` tiene dependencias duplicadas de validation.
   - No rompe necesariamente, pero agrega ruido.
   - Recomendacion: limpiar dependencias duplicadas.

4. Manejo global de errores muy general.
   - `RuntimeException` se transforma siempre en 404.
   - Recomendacion: errores de dominio tipados (`NotFound`, `Conflict`, `Forbidden`, `BadRequest`).

5. Logging de seguridad en consola.
   - `JwtAuthFilter` imprime subject/role y stack traces.
   - Recomendacion: usar logger con niveles y evitar datos sensibles en produccion.

6. Token storage web en `localStorage`.
   - Es simple, pero expone tokens ante XSS.
   - Recomendacion: evaluar cookies HttpOnly/SameSite para web o reducir vida del access token.

7. No hay paginacion ni filtros server-side.
   - Listar todos los pacientes puede escalar mal.
   - Recomendacion: `GET /api/patients?search=&page=&size=`.

8. CORS esta hardcodeado.
   - Recomendacion: mover origenes permitidos a configuracion por entorno.

### Hallazgos de prioridad baja

1. Textos y codigo mezclan espanol/ingles.
   - Recomendacion: definir idioma de dominio y de UI; internacionalizacion futura.

2. Algunos documentos antiguos tienen mojibake.
   - Recomendacion: normalizar a UTF-8 o ASCII consistente.

3. No hay versionado de API.
   - Recomendacion: `/api/v1/...` cuando el contrato empiece a estabilizarse.

4. No hay telemetria de errores.
   - Recomendacion: logging centralizado y crash/error reporting para frontend.

## Funcionalidades utiles para agregar

### Paneles para doctores

- Dashboard general con:
  - pacientes activos;
  - pacientes sin registros hoy;
  - pacientes con balance/UF fuera de rango;
  - ultimos cambios cargados;
  - alertas pendientes.
- Graficos por paciente:
  - tendencia diaria de ultrafiltracion;
  - promedio semanal/mensual;
  - total infusion vs drenaje;
  - adherencia: dias con cambios completos vs dias incompletos;
  - uso de concentraciones;
  - observaciones frecuentes.
- Comparacion temporal:
  - mes actual vs mes anterior;
  - semana actual vs semana anterior;
  - deteccion de cambios abruptos.
- Panel de seguimiento:
  - pacientes priorizados por riesgo;
  - notas del doctor;
  - proxima revision;
  - estado: estable, observar, contactar, urgente.

### Notificaciones y alertas

- Recordatorios locales para que el paciente registre cada cambio.
- Alerta si el paciente no registra nada en el dia.
- Alerta si faltan bolsas/cambios esperados.
- Alerta por UF/balance fuera de rango configurable por doctor.
- Alerta por observaciones con palabras clave: dolor, fiebre, turbio, sangre, mareo.
- Notificacion al doctor cuando un paciente cumple condicion de riesgo.
- Resumen diario para el doctor al final del dia.

### Funciones clinicas adicionales

- Registro de peso, presion arterial, temperatura y glucemia.
- Registro de aspecto del liquido drenado.
- Registro de sintomas.
- Objetivos o rangos personalizados por paciente.
- Adjuntar estudios/laboratorio.
- Exportacion PDF con firma/datos del doctor.
- Reporte longitudinal de 3, 6 y 12 meses.

### Seguridad y privacidad

- Flujo de invitacion doctor-paciente.
- Consentimiento explicito del paciente para compartir historial.
- Auditoria de accesos: quien vio o modifico datos.
- Recuperacion de contrasena.
- Verificacion de email.
- Politicas de contrasena y bloqueo por intentos fallidos.
- Roles administrativos reales.

### Operacion y calidad

- Migraciones con Flyway/Liquibase.
- Docker Compose para backend + Postgres + frontend.
- CI con build backend, `dart analyze`, tests y build web.
- Seeds de desarrollo reproducibles.
- OpenAPI/Swagger documentado.
- Tests de integracion con Testcontainers.
- Monitoreo de salud (`/actuator/health`).

## Roadmap sugerido

### Fase 1 - Estabilizacion

- Arreglar Maven wrapper.
- Corregir `logout/all`.
- Mover secretos a variables de entorno.
- Agregar tests de auth y sesiones.
- Confirmar convencion de UF/balance.
- Agregar paginacion/busqueda de pacientes.

### Fase 2 - Producto clinico minimo

- Dashboard del doctor con alertas basicas.
- Notificaciones de registro faltante.
- Rangos de UF configurables por paciente.
- Consentimiento de vinculacion doctor-paciente.
- Exportacion PDF mejorada.

### Fase 3 - Seguimiento avanzado

- Graficos longitudinales.
- Registro de signos vitales y sintomas.
- Alertas por reglas clinicas.
- Auditoria de accesos.
- Panel administrativo.

### Fase 4 - Produccion

- Migraciones versionadas.
- Deploy reproducible.
- Observabilidad.
- Politicas de seguridad web.
- Backups y restauracion.
- Documentacion legal/privacidad para datos de salud.

## Verificaciones recientes

- Frontend: `dart analyze` sin issues.
- Frontend: `flutter build web` compilo correctamente; Flutter informo advertencias de Wasm por dependencias web (`flutter_secure_storage_web`, `universal_html`).
- Backend: no se pudo compilar por falla del Maven wrapper en Windows antes de ejecutar Maven.

## Notas de mantenimiento

- El frontend usa `API_BASE_URL` por `--dart-define`; si no se define, apunta a `http://localhost:8080`.
- El backend actual espera PostgreSQL local segun `application.properties`.
- Los README de backend y frontend contienen detalle especifico de cada subproyecto.
