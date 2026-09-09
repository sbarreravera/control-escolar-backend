# Control Escolar API

Backend del sistema **Control Escolar**, una plataforma orientada al registro de entradas y salidas de alumnos mediante credenciales con código QR y al envío de notificaciones a padres o tutores.

El proyecto está diseñado como una aplicación monolítica modular construida con **Spring Boot**, **PostgreSQL** y **Flyway**, con una API REST consumida por un frontend desarrollado en Angular y CoreUI.

> Estado actual: **en desarrollo**

---

## Objetivo

Centralizar la administración escolar necesaria para:

- Registrar escuelas, alumnos y tutores.
- Asociar alumnos con uno o varios padres o tutores.
- Generar y administrar credenciales con códigos QR.
- Registrar entradas y salidas mediante cámara, lector QR USB o captura manual.
- Notificar a los tutores cuando un alumno entra o sale de la escuela.
- Mantener un historial auditable de accesos y notificaciones.
- Facilitar la importación inicial de información desde archivos Excel.
- Proporcionar reportes básicos de operación.

---

## Arquitectura

El backend utiliza una arquitectura **monolítica modular**.

Cada dominio se organiza de forma independiente por responsabilidad:

```text
com.graduacionesisamar.controlescolar
├── school
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── student
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── guardian
├── credential
├── access
├── notification
├── importation
├── security
├── configuration
└── shared
```

La separación por dominio permite mantener el proyecto sencillo durante el MVP y facilita una futura extracción de módulos si el crecimiento del sistema lo requiere.

---

## Tecnologías

- Java 25 LTS
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Bean Validation
- Spring Boot Actuator
- Lombok
- Maven Wrapper
- Git y GitHub

---

## Estado funcional

### Implementado

- Configuración base de Spring Boot.
- Conexión con PostgreSQL mediante variables de entorno.
- Control de versiones de base de datos con Flyway.
- Health check con Spring Boot Actuator.
- Registro y consulta de escuelas.
- Validación de códigos de escuela duplicados.
- Registro y consulta de alumnos.
- Consulta de alumnos por escuela.
- Consulta de alumnos por identificador.
- Validación de matrícula duplicada dentro de una escuela.
- Validación de escuela inexistente.
- Pruebas manuales mediante Postman.

### En desarrollo

- Actualización y baja lógica de escuelas y alumnos.
- Administración de padres y tutores.
- Asociación entre alumnos y tutores.
- Generación y administración de credenciales QR.
- Registro de entradas y salidas.
- Notificaciones push con Firebase Cloud Messaging.
- Importación masiva desde Excel.
- Autenticación y autorización.
- Reportes.
- Documentación OpenAPI/Swagger.
- Pruebas automatizadas.

---

## Modelo de datos inicial

La primera migración crea las siguientes tablas:

```text
schools
app_users
students
guardians
student_guardians
credentials
access_events
guardian_devices
notification_logs
flyway_schema_history
```

Las migraciones se encuentran en:

```text
src/main/resources/db/migration
```

Formato utilizado:

```text
V1__create_initial_schema.sql
V2__description_of_change.sql
V3__description_of_change.sql
```

Las migraciones ya ejecutadas no deben modificarse. Cada cambio posterior de base de datos debe agregarse mediante una nueva versión.

---

## Requisitos

Antes de ejecutar el proyecto se requiere:

- Java 25
- PostgreSQL 18
- Git

No es necesario instalar Maven globalmente porque el repositorio incluye **Maven Wrapper**.

Verificación:

```powershell
java --version
javac --version
```

---

## Configuración de PostgreSQL

Ejemplo de creación de usuario y base de datos:

```sql
CREATE USER control_escolar_app
WITH PASSWORD 'change_this_password';

CREATE DATABASE control_escolar
WITH
    OWNER = control_escolar_app
    ENCODING = 'UTF8';

GRANT ALL PRIVILEGES
ON DATABASE control_escolar
TO control_escolar_app;
```

No se recomienda utilizar el usuario administrador `postgres` desde la aplicación.

---

## Variables de entorno

La aplicación utiliza las siguientes variables:

| Variable | Requerida | Valor predeterminado | Descripción |
|---|---:|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/control_escolar` | URL JDBC de PostgreSQL |
| `DB_USERNAME` | No | `sambarve` | Usuario de conexión |
| `DB_PASSWORD` | Sí | Sin valor | Contraseña de la base de datos |
| `SERVER_PORT` | No | `8080` | Puerto HTTP de la aplicación |
| `GUARDIAN_COOKIE_SECURE` | No | `true` | Exige HTTPS para la cookie de sesión del tutor; usar `false` únicamente en desarrollo local por HTTP |

La contraseña no debe agregarse a `application.properties`, al historial de Git ni a la documentación pública.

Ejemplo en PowerShell:

```powershell
$securePassword = Read-Host "PostgreSQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
```

Opcionalmente:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/control_escolar"
$env:DB_USERNAME = "control_escolar_app"
$env:SERVER_PORT = "8080"
```

---

## Ejecución local

Clonar el repositorio:

```powershell
git clone https://github.com/sbarreravera/control-escolar-backend.git
cd control-escolar-backend
```

Compilar:

```powershell
.\mvnw.cmd clean compile
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP"
}
```

---

## API disponible

Base path:

```text
/api/v1
```

### Escuelas

#### Crear una escuela

```http
POST /api/v1/schools
Content-Type: application/json
```

```json
{
  "name": "Escuela de Prueba",
  "code": "ESC-001"
}
```

Respuesta:

```http
201 Created
```

#### Listar escuelas

```http
GET /api/v1/schools
```

Respuesta:

```http
200 OK
```

### Alumnos

#### Crear un alumno

```http
POST /api/v1/students
Content-Type: application/json
```

```json
{
  "schoolId": 1,
  "enrollmentNumber": "MAT-001",
  "firstName": "Samuel",
  "lastName": "Barrera Vera",
  "gradeName": "1",
  "groupName": "A"
}
```

Respuesta:

```http
201 Created
```

#### Listar alumnos por escuela

```http
GET /api/v1/students?schoolId=1
```

Respuesta:

```http
200 OK
```

#### Consultar alumno por identificador

```http
GET /api/v1/students/1
```

Respuesta:

```http
200 OK
```

---

## Respuestas de error

La API utiliza códigos HTTP convencionales:

| Código | Uso |
|---:|---|
| `200` | Consulta exitosa |
| `201` | Recurso creado |
| `400` | Datos de entrada inválidos |
| `404` | Recurso no encontrado |
| `409` | Conflicto por información duplicada |
| `500` | Error interno no controlado |

Durante el desarrollo todavía puede mostrarse información técnica extensa en las respuestas de error. Antes de producción se agregará un manejador global para estandarizar las respuestas y ocultar trazas internas.

Formato objetivo:

```json
{
  "timestamp": "2026-08-03T21:00:00-06:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found",
  "path": "/api/v1/students/999"
}
```

---

## Pruebas con Postman

Estructura sugerida:

```text
Control Escolar
└── Localhost
    ├── Schools
    │   ├── POST - Crear escuela
    │   ├── GET - Listar escuelas
    │   └── POST - Validar código duplicado
    └── Students
        ├── POST - Crear alumno
        ├── GET - Listar alumnos por escuela
        ├── GET - Consultar alumno por id
        ├── POST - Validar matrícula duplicada
        └── POST - Validar escuela inexistente
```

Environment recomendado:

```text
baseUrl=http://localhost:8080
apiBasePath=/api/v1
schoolId=
studentId=
```

No deben exportarse contraseñas, tokens privados ni credenciales dentro de la colección.

---

## Convenciones de desarrollo

- Nombres de clases, métodos y variables en inglés.
- Un máximo recomendado de 25 líneas por método.
- Preferir objetos de entrada cuando un método requiera más de tres parámetros.
- Agregar documentación a clases y métodos públicos.
- No exponer entidades JPA directamente desde los controladores.
- Utilizar DTO para solicitudes y respuestas.
- Mantener relaciones JPA con carga diferida cuando sea apropiado.
- Usar transacciones en la capa de servicio.
- No habilitar `spring.jpa.open-in-view`.
- Los cambios de base de datos deben realizarse exclusivamente mediante Flyway.
- No almacenar credenciales dentro del repositorio.

---

## Flujo de ramas

```text
main
develop
```

- `main`: versión estable.
- `develop`: integración del desarrollo diario.
- Las funcionalidades nuevas pueden trabajarse en ramas `feature/*`.
- Las correcciones urgentes pueden trabajarse en ramas `hotfix/*`.

Ejemplo:

```powershell
git switch develop
git pull
git switch -c feature/guardian-management
```

---

## Seguridad

Antes de desplegar en producción se debe:

- Configurar Spring Security.
- Implementar autenticación basada en JWT o sesión segura.
- Proteger endpoints por roles.
- Restringir los endpoints de Actuator.
- Desactivar la exposición de trazas de error.
- Utilizar HTTPS.
- Guardar secretos en variables de entorno o un gestor de secretos.
- Utilizar contraseñas diferentes para desarrollo y producción.
- Aplicar políticas CORS explícitas.
- Rotar cualquier credencial que haya sido expuesta.
- Realizar respaldos periódicos de PostgreSQL.

---

## Despliegue previsto

Arquitectura objetivo:

```text
Internet
   │
   ▼
Nginx
   ├── Angular PWA
   └── Spring Boot API
           │
           ▼
      PostgreSQL
```

Componentes previstos:

- Angular y CoreUI para el frontend.
- Spring Boot como API REST.
- PostgreSQL como base de datos.
- Nginx como servidor web y reverse proxy.
- Docker Compose para ejecución y despliegue.
- Firebase Cloud Messaging para notificaciones push.
- Certificados TLS mediante Let's Encrypt.

Subdominios previstos:

```text
escolar.graduacionesisamar.com
api-escolar.graduacionesisamar.com
```

---

## Roadmap

### Fase 1: base operativa

- [x] Configuración del proyecto.
- [x] PostgreSQL y Flyway.
- [x] Módulo de escuelas.
- [x] Módulo inicial de alumnos.
- [ ] Módulo de tutores.
- [ ] Relación alumno-tutor.
- [ ] Credenciales QR.
- [ ] Registro de entradas y salidas.

### Fase 2: notificaciones

- [ ] Registro de dispositivos.
- [ ] Integración con Firebase.
- [ ] Envío de notificaciones.
- [ ] Historial y reintentos.

### Fase 3: operación escolar

- [ ] Importación desde Excel.
- [ ] Administración de usuarios.
- [ ] Roles y permisos.
- [ ] Reportes.
- [ ] Dashboard.

### Fase 4: producción

- [ ] Pruebas automatizadas.
- [ ] OpenAPI/Swagger.
- [ ] Docker Compose.
- [ ] Nginx y HTTPS.
- [ ] Monitoreo y respaldos.
- [ ] Despliegue en VPS.

---

## Repositorios relacionados

Frontend:

```text
https://github.com/sbarreravera/control-escolar-frontend
```

Backend:

```text
https://github.com/sbarreravera/control-escolar-backend
```

---

## Licencia

Este repositorio no incluye actualmente una licencia de uso.

Hasta que se agregue una licencia explícita, el código se considera de uso reservado por su propietario. Las dependencias utilizadas conservan sus respectivas licencias.

---

## Autor

**Samuel Barrera Vera**

Proyecto en desarrollo para la administración y control de accesos escolares.
