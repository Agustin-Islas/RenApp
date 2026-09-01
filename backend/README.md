# RenApp - Backend Service

Este es el servicio Backend de **RenApp**, una plataforma integral diseñada para el registro, consulta y seguimiento clínico del tratamiento de diálisis peritoneal. 

La arquitectura está diseñada como una API RESTful robusta y escalable, encargada de centralizar la lógica de negocio, proteger la información sensible de salud y proveer datos consistentes a las interfaces de usuario.

## 🚀 Tecnologías Principales

- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3.x (Spring Web MVC, Spring Data JPA)
- **Seguridad:** Spring Security con JSON Web Tokens (JWT) Stateless
- **Base de Datos:** PostgreSQL
- **ORM:** Hibernate
- **Herramientas Adicionales:** Lombok, Maven Wrapper

## 🏗️ Arquitectura y Patrones

El proyecto sigue un diseño por capas (Layered Architecture) para mantener la separación de responsabilidades:
- **Controllers:** Manejo de peticiones HTTP y mapeo de endpoints.
- **Services:** Reglas de negocio clínicas y validaciones de dominio.
- **Repositories:** Persistencia de datos mediante abstracciones de Spring Data JPA.
- **Security:** Filtros de autorización basados en roles (Patient, Doctor) e interceptores JWT.
- **DTOs & Mappers:** Transferencia segura de datos sin exponer las entidades internas de la base de datos.

## ⚙️ Configuración y Ejecución Local

### Prerrequisitos
- JDK 21 instalado.
- Servidor PostgreSQL en ejecución.
- Base de datos local creada (por defecto `dialysis_db`).

### Variables de Entorno
El sistema requiere de configuración mediante variables de entorno (o localizadas en un archivo `.env` en la raíz de la carpeta `backend`):
- `DB_URL` (ej: `jdbc:postgresql://localhost:5432/dialysis_db`)
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`

### Ejecutar el Proyecto
Para iniciar el servidor de desarrollo local, utilizar Maven Wrapper:

```bash
cd backend

# En Windows:
.\mvnw.cmd spring-boot:run

# En Linux / Mac:
./mvnw spring-boot:run
```

El servidor iniciará por defecto en el puerto `8081`.

---
*Nota: Para detalles específicos sobre decisiones de arquitectura, diseño Multi-Tenant o flujos de despliegue, los desarrolladores autorizados deben referirse al Vault interno de documentación del repositorio.*
