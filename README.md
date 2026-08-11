# Academic Reminder — Monorepositorio

Aplicación completa para organizar cursos, actividades, asistencia y recordatorios académicos. El repositorio contiene el frontend móvil/web y los dos microservicios del backend final.

## Estructura

```text
frontend/             Expo + React Native Web
users/                Microservicio de perfiles de usuario (puerto interno 8081)
academic-reminder/    Microservicio del dominio académico (puerto interno 8082)
nginx/                API Gateway público (localhost:9090)
pgadmin/              Registro automático de las dos bases PostgreSQL
postman/               Colección y ambiente de pruebas de la API
docs/                  Requisitos, arquitectura, ADR, evidencias y defensa
compose.yml            Orquestación completa
compose.production.yml Backend sin dependencias locales para un host de producción
```

El backend está separado en dos módulos y dos bases de datos:

- `users_db`: perfiles sincronizados con AWS Cognito y auditoría de usuarios.
- `academic_reminder_db`: cursos, membresías, actividades, asistencias, recordatorios, notificaciones, prioridades y auditoría académica.

Las tablas se crean mediante `database/init.sql`. En cada arranque, el servicio de preparación ejecuta también `academic-reminder/database/upgrade.sql`, que es idempotente y permite actualizar volúmenes existentes. Este proyecto **no utiliza Flyway**.

La actualización conserva los cursos duplicados antiguos y añade su código de clase al nombre para distinguirlos. Después instala una restricción atómica que rechaza nuevos duplicados con HTTP 409.

## Funcionalidades principales

- Registro seleccionable como estudiante o profesor, confirmación de correo, recuperación de contraseña e inicio de sesión con AWS Cognito.
- Asignación post-confirmación que mantiene a cada cuenta en un solo grupo (`STUDENT` o `ADMIN`) y sincroniza el perfil con `users_db` por el `sub` de Cognito.
- Autorización con solo dos roles: `STUDENT` y `ADMIN`; el rol `ADMIN` representa al profesor y no es un superusuario global.
- Matriz de permisos explícita: ambos roles crean recordatorios personales; solo `ADMIN` crea clases y avisos de clase; solo `STUDENT` se une mediante código.
- Aislamiento por propietario: cada profesor administra únicamente sus clases y cada usuario únicamente sus recordatorios personales.
- Edición de actividades, historial de completadas/vencidas y alerta visual durante sus últimas 24 horas.
- Unicidad de cursos por profesor: no se repite la misma combinación normalizada de nombre y descripción.
- Perfiles de usuario en un microservicio independiente.
- Creación y unión a cursos mediante código único.
- Actividades, asistencias y recordatorios personales o de curso.
- Número de actividad consecutivo por profesor, independiente de la clave primaria global.
- Selector de estudiantes inscritos y aislamiento de asistencia por usuario.
- Alertas configurables y prioridades personalizadas.
- Excepciones propias, manejadores globales, auditoría y logs estructurados.
- Pruebas unitarias y funcionales en ambos microservicios.

## Configuración local

1. Crea el archivo local de variables:

```powershell
Copy-Item .env.example .env
```

2. Completa en `.env` los datos del User Pool y App Client de Cognito. El archivo `.env` está ignorado por Git.

3. Inicia todo el monorepositorio y espera las comprobaciones de salud:

```powershell
docker compose up -d --build --wait
```

Servicios disponibles:

- Aplicación: <http://localhost:3000>
- API Gateway: <http://localhost:9090>
- pgAdmin: <http://localhost:9090/pgadmin/>
- Salud de usuarios: <http://localhost:9090/health/users>
- Salud académica: <http://localhost:9090/health/academic-reminder>

`academic-reminder-db-setup` termina en estado `Exited (0)` después de aplicar el SQL; ese estado es correcto. Los demás servicios deben aparecer como `healthy` en `docker compose ps -a`.

Para aplicar cambios posteriores sin borrar datos, repite `docker compose up -d --build --wait`. No uses `docker compose down -v` salvo que quieras eliminar definitivamente ambas bases.

## Pruebas del backend

```powershell
cd users
./gradlew test

cd ../academic-reminder
./gradlew test
```

En Windows también se puede usar `gradlew.bat test` dentro de cada módulo.

## Validación del frontend

```powershell
cd frontend
npm ci
npm run typecheck
npm run lint
npm run build:web
```

Para utilizar Expo Go:

```powershell
npm start
```

Para probar con Expo Go contra el backend público de AWS, sin levantar backend ni base local:

```powershell
npm run start:aws
```

Expo Go todavía necesita el servidor de desarrollo Metro en la computadora; las API, autenticación y bases de datos usadas por ese comando están completamente en AWS.

Antes del desarrollo nativo, copia `frontend/.env.example` a `frontend/.env` y configura el App Client público de Cognito. El frontend detecta la dirección del equipo y utiliza el gateway del puerto `9090`. En la compilación web de Docker, `/api` se reenvía internamente al gateway Nginx del mismo `compose`.

## Producción sin dependencias locales

`compose.production.yml` levanta únicamente `users`, `academic-reminder`, Nginx y dos tareas SQL de preparación. No incluye frontend, pgAdmin, PostgreSQL local ni datos demo. Las dos bases deben existir en PostgreSQL externo —por ejemplo Amazon RDS— y ser accesibles desde el host del backend.

1. Copia `.env.production.example` a `.env.production` y completa las credenciales externas, Cognito y el origen HTTPS exacto del frontend.
2. Configura el frontend desplegado con `EXPO_PUBLIC_API_URL` apuntando a la URL HTTPS pública del gateway y con el mismo App Client de Cognito.
3. Inicia el backend:

```powershell
docker compose -f compose.production.yml --env-file .env.production up -d --build --wait
```

El TLS debe terminar en el balanceador o proxy público de AWS. Las contraseñas reales solo se guardan en `.env.production` del servidor o en un gestor de secretos; nunca se incluyen en Git.

### Entorno AWS activo

- Aplicación web: <https://d29ydq13vz3ryv.cloudfront.net>
- APK Android: <https://d29ydq13vz3ryv.cloudfront.net/AcademicReminder.apk>
- API pública: <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com>
- Salud: <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com/health/users> y <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com/health/academic-reminder>

El bundle público se construye con esas URLs y no contiene referencias al backend local. El perfil `production-apk` de `frontend/eas.json` genera un APK instalable con la misma configuración.

## Documentación de evaluación

- [Requisitos y trazabilidad](docs/requirements-and-traceability.md)
- [Arquitectura, datos y escalamiento](docs/architecture.md)
- [Pruebas y evidencia](docs/testing-evidence.md)
- [Cumplimiento de la rúbrica P01](docs/rubric-compliance.md)
- [GitFlow, priorización y CI](docs/development-workflow.md)
- [Entrega Android y Expo Go](docs/mobile-release.md)
- [Decisiones de arquitectura](docs/adr/0001-two-cognito-roles.md)
- [Modelo de negocio](docs/business-model.md)
- [Guía de defensa](docs/defense-guide.md)
- [Pendientes y traspaso de Jira](docs/jira-handoff.md)

## Integración continua

GitHub Actions valida por separado ambos microservicios, TypeScript, lint, el bundle web y la sintaxis de Docker Compose en `main`, `develop` y sus pull requests.
