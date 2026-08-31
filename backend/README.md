# Backend - RenApp System

Backend REST para RenApp. Expone autenticacion, autorizacion por rol, gestion de pacientes/doctores y registro de cambios de dialisis peritoneal.

## Mision del backend

El backend debe ser la fuente confiable de datos clinicos del sistema: proteger informacion sensible, validar reglas de negocio, preservar trazabilidad y entregar datos ordenados para que el paciente registre su tratamiento y el doctor pueda hacer seguimiento.

## Estado actual

Implementado:

- Spring Boot con arquitectura por capas: controller, service, repository, mapper, dto y model.
- Persistencia con PostgreSQL y JPA/Hibernate.
- Seguridad stateless con JWT.
- Access token y refresh token.
- Registro y login de pacientes.
- Registro y login de doctores.
- Perfil propio:
  - `GET /api/patients/me`
  - `GET /api/doctors/me`
- Asociacion de pacientes al doctor autenticado:
  - listar pacientes propios;
  - agregar paciente;
  - quitar paciente.
- Endpoints CRUD administrados para pacientes, doctores y sesiones.
- Registro de sesiones bajo paciente.
- Consulta de sesiones por dia, rango y mes.
- Resumen de sesiones por dia y mes.
- Soft delete de pacientes y sesiones con campo `active`.
- Validacion de concentraciones fijas: `1.5`, `2.3`, `3.8`.
- Concentraciones personalizadas por paciente.
- CORS configurado para desarrollo local y deploys conocidos.
- Manejo global de errores con DTO estandar.

## Tecnologias

- Java 21.
- Spring Boot 4.0.0.
- Spring Web MVC.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- PostgreSQL.
- Lombok.
- JJWT.
- Maven Wrapper.

## Estructura

```text
backend-dialysis-record/
+-- backend-dialysis-record/
    +-- pom.xml
    +-- src/main/java/com/agustin/backend_dialysis_record/
    |   +-- controller/
    |   +-- dto/
    |   +-- mapper/
    |   +-- model/
    |   +-- repository/
    |   +-- security/
    |   +-- service/
    +-- src/main/resources/application.properties
```

## Configuracion local

Archivo actual:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dialysis_db
spring.datasource.username=dialysis_user
spring.datasource.password=dialysis_pass
security.jwt.issuer=dialysis-record
```

Requisitos:

- PostgreSQL corriendo.
- Base de datos `dialysis_db`.
- Usuario `dialysis_user`.
- Password `dialysis_pass`.
- Java 21.

Para produccion, mover credenciales y secreto JWT a variables de entorno o un gestor de secretos.

## Ejecucion

Ruta del proyecto Spring:

```powershell
cd "backend-dialysis-record\backend-dialysis-record"
```

Comando esperado:

```powershell
.\mvnw.cmd spring-boot:run
```

Compilacion:

```powershell
.\mvnw.cmd -DskipTests compile
```

Nota: durante la auditoria, `mvnw.cmd` fallo en Windows con `Cannot start maven from wrapper` antes de iniciar Maven. Si ocurre, revisar el wrapper, instalar Maven globalmente o regenerar el wrapper.

## Endpoints principales

### Auth

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/register/patient`
- `POST /auth/register/doctor`
- `POST /auth/logout`
- `POST /auth/logout/all`

### Pacientes

- `GET /api/patients/me`
- `GET /api/patients`
- `GET /api/patients/{patientId}`
- `PUT /api/patients/{patientId}`
- `DELETE /api/patients/{patientId}`
- `PATCH /api/patients/{patientId}/activate`

### Sesiones bajo paciente

- `POST /api/patients/{patientId}/sessions`
- `GET /api/patients/{patientId}/sessions`
- `GET /api/patients/{patientId}/sessions/day/{day}`
- `GET /api/patients/{patientId}/sessions/summary/day/{day}`
- `GET /api/patients/{patientId}/sessions/summary/month?year=YYYY&month=M`

### Sesiones directas

- `GET /api/sessions`
- `GET /api/sessions/{sessionId}`
- `PUT /api/sessions/{sessionId}`
- `DELETE /api/sessions/{sessionId}`

### Doctores

- `GET /api/doctors/me`
- `GET /api/doctors/me/patients`
- `POST /api/doctors/me/patients/{patientId}`
- `DELETE /api/doctors/me/patients/{patientId}`
- `GET /api/doctors`
- `POST /api/doctors`
- `GET /api/doctors/{doctorId}`
- `PUT /api/doctors/{doctorId}`
- `DELETE /api/doctors/{doctorId}`
- `PATCH /api/doctors/{doctorId}/activate`
- `GET /api/doctors/{doctorId}/patients`
- `POST /api/doctors/{doctorId}/patients/{patientId}`
- `DELETE /api/doctors/{doctorId}/patients/{patientId}`

## Seguridad y autorizacion

El sistema usa JWT con `role` en el claim del token. El filtro JWT transforma ese rol en authority Spring (`ROLE_PATIENT`, `ROLE_DOCTOR`, etc.).

Reglas actuales:

- Pacientes acceden a su propio perfil y sesiones.
- Doctores acceden a sus pacientes asociados.
- Doctores pueden listar pacientes para asociarlos.
- Endpoints administrativos usan rol `ADMIN`, aunque no hay UI admin completa.
- `AuthzService` valida ownership de paciente, doctor y sesion.

## Modelo de dominio

### UserAccount

- `email`
- `passwordHash`
- `role`
- `enabled`
- vinculo one-to-one con `Doctor` o `Patient`

### Doctor

- datos personales basicos;
- lista de pacientes asociados.

### Patient

- datos personales;
- doctor asociado;
- lista de sesiones;
- concentraciones personalizadas;
- soft delete con `active`.

### Session

- fecha;
- hora;
- numero de bolsa;
- concentracion;
- infusion;
- drenaje;
- parcial;
- observaciones;
- paciente propietario.

## Reglas de negocio actuales

- El email de login/registro se normaliza con `trim().toLowerCase()`.
- El paciente puede tener concentraciones personalizadas ademas de las fijas.
- Una sesion solo acepta concentraciones permitidas para el paciente.
- El parcial se recalcula en persistencia y actualizacion.
- Las sesiones se consultan ordenadas por fecha/hora.

## Riesgos y mejoras recomendadas

Prioridad alta:

- Corregir `logout/all`: actualmente las rutas `/auth/**` quedan fuera del filtro JWT y permitidas antes de la regla autenticada.
- Agregar consentimiento o aprobacion para asociar doctor-paciente.
- Mover secreto JWT y credenciales fuera de `application.properties`.
- Reemplazar `ddl-auto=update` por migraciones.
- Confirmar clinicamente si `partial = infusion - drainage` es el signo correcto para UF/balance.

Prioridad media:

- Regenerar o arreglar Maven Wrapper.
- Agregar tests de integracion para auth, ownership, sesiones y resumenes.
- Agregar paginacion y busqueda server-side en pacientes.
- Limpiar dependencias duplicadas en `pom.xml`.
- Usar logger en vez de `System.out.println`/`printStackTrace`.
- Mejorar tipos de error de dominio.

Prioridad baja:

- Agregar versionado `/api/v1`.
- Documentar contrato con OpenAPI/Swagger.
- Normalizar idioma y codificacion de textos.

## Roadmap backend

1. Estabilizar seguridad:
   - `logout/all`;
   - refresh token;
   - bloqueo por intentos fallidos;
   - recuperacion de password.
2. Mejorar privacidad:
   - invitacion doctor-paciente;
   - consentimiento;
   - auditoria de accesos.
3. Soportar dashboards:
   - endpoints agregados para tendencias;
   - pacientes sin registros;
   - alertas por reglas.
4. Preparar produccion:
   - perfiles por entorno;
   - migraciones;
   - Docker Compose;
   - CI;
   - observabilidad.

## Verificacion reciente

- No se pudo ejecutar compilacion Maven por falla del wrapper.
- Cambios Flutter relacionados fueron verificados con `dart analyze`.
