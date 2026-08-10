# Arquitectura y modelo

## Vista de componentes

```mermaid
flowchart LR
    U["Web / Android"] --> C["Amazon Cognito"]
    C --> PC["Lambda post-confirmación"]
    PC --> G["Grupos ADMIN / STUDENT"]
    U --> CF["CloudFront + S3"]
    U --> APIGW["API Gateway HTTPS"]
    APIGW --> N["Nginx en EC2"]
    N --> US["users :8081"]
    N --> AR["academic-reminder :8082"]
    AR --> US
    US --> RDS1["RDS PostgreSQL / users_db"]
    AR --> RDS2["RDS PostgreSQL / academic_reminder_db"]
    US --> CW["CloudWatch Logs"]
    AR --> CW
    N --> CW
    SM["Secrets Manager"] --> EC2["EC2 + Docker Compose"]
    EC2 --> N
```

El frontend estático no comparte host ni almacenamiento con el backend. API Gateway proporciona HTTPS y Nginx enruta `/users/**` y `/academic-reminder/**` a los microservicios internos. RDS no es público y Systems Manager sustituye al acceso SSH.

## Registro, identidad y perfil

1. Web o APK envía correo, nombre y el rol elegido (`ADMIN` o `STUDENT`) a Cognito; la contraseña nunca llega a los microservicios.
2. Al confirmar la cuenta, la Lambda valida `custom:role`, agrega el grupo seleccionado y elimina el contrario. Si el atributo es inválido, usa `STUDENT`.
3. Cognito entrega tokens JWT con el grupo. El backend valida firma, emisor, cliente y rol en cada solicitud protegida.
4. La primera consulta a `/users/me` sincroniza `sub`, correo, nombre y rol en `users_db`; ese perfil queda relacionado con los datos académicos mediante el `sub` de Cognito.
5. La app móvil y la web nunca se conectan directamente a PostgreSQL. En Docker se usan dos PostgreSQL locales; en producción los microservicios usan las dos bases privadas de RDS.

## Capas del backend

```mermaid
flowchart TD
    Controller --> DTO
    Controller --> Service
    Service --> AccessService
    Service --> Repository
    Service --> Mapper
    Repository --> PostgreSQL
    Service --> AuditService
    ExceptionHandler --> ErrorResponse
```

- Controller: contrato HTTP, códigos de estado y autorización declarativa.
- Service: reglas de negocio, pertenencia y transacciones.
- AccessService: decisiones reutilizables de propietario, rol e inscripción.
- Repository: consultas e índices del modelo persistente.
- Mapper/DTO: separa el contrato público de las entidades.
- ExceptionHandler: transforma fallos esperados a respuestas consistentes.

## Modelo de datos académico

```mermaid
erDiagram
    COURSES ||--o{ COURSE_MEMBERSHIPS : contiene
    COURSES ||--o{ REMINDERS : publica
    COURSES ||--o{ ACTIVITIES : programa
    COURSES ||--o{ ATTENDANCE : registra
    PRIORITY_CATEGORIES ||--o{ REMINDERS : clasifica
    REMINDERS ||--o{ STUDENT_REMINDER_STATES : personaliza
    REMINDERS ||--o{ NOTIFICATIONS : agenda
    ACTIVITIES ||--o{ ACTIVITY_COMPLETIONS : completa

    COURSES {
      bigint id PK
      varchar join_code UK
      varchar professor_user_id
      varchar name
      varchar description
    }
    COURSE_MEMBERSHIPS {
      bigint id PK
      bigint course_id FK
      varchar student_user_id
    }
    REMINDERS {
      bigint id PK
      bigint course_id FK
      varchar owner_user_id
      varchar created_by_user_id
      timestamptz due_at
      varchar status
    }
    ACTIVITIES {
      bigint id PK
      bigint course_id FK
      varchar created_by_user_id
      bigint activity_number
      timestamptz due_at
    }
    ACTIVITY_COUNTERS {
      varchar owner_user_id PK
      bigint last_value
    }
```

Los identificadores primarios son globales porque garantizan integridad y relaciones. `activity_number` es el identificador visible por profesor; se obtiene mediante un contador atómico independiente y tiene unicidad `(created_by_user_id, activity_number)`.

La unicidad de curso usa un índice funcional por profesor, nombre normalizado y descripción normalizada. La verificación del servicio mejora el mensaje, mientras que la restricción SQL elimina condiciones de carrera.

## Estados y errores HTTP

| Estado | Uso |
|---:|---|
| 200 | Consulta o actualización con cuerpo |
| 201 | Creación exitosa |
| 204 | Eliminación exitosa sin cuerpo |
| 400 | DTO, fecha, transición o regla inválida |
| 401 | Token ausente, vencido o inválido |
| 403 | Rol, propietario o inscripción insuficiente |
| 404 | Recurso inexistente o no visible |
| 409 | Duplicado o conflicto de integridad |
| 502/503 | Dependencia interna o proveedor de identidad no disponible |
| 500 | Falla inesperada registrada; no debe usarse para reglas de negocio |

## Escalamiento

Escalamiento vertical aumenta CPU/RAM de EC2 o la clase de RDS. Es simple y apropiado para la carga académica inicial, pero tiene un límite físico y puede requerir reinicio.

Escalamiento horizontal ejecutaría varias réplicas sin estado de `users` y `academic-reminder` detrás de un Application Load Balancer o ECS. Las sesiones ya viven en Cognito y los datos en RDS, por lo que los servicios pueden replicarse. Para esa fase se debe externalizar cualquier tarea programada con bloqueo distribuido, usar RDS Multi-AZ/read replicas según el patrón de carga y configurar auto scaling por CPU, latencia y número de solicitudes.

La versión actual prioriza costo y claridad de evaluación: una EC2 `t3.small` y una RDS privada `db.t4g.micro`. El diseño conserva el camino de migración horizontal sin cambiar los contratos de la API.
