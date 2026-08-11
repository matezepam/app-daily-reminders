# Requisitos y trazabilidad

## Alcance

Daily Reminder organiza clases, actividades, asistencia y recordatorios. La solución tiene dos actores autenticados:

- `ADMIN`: representa al profesor. Crea y administra únicamente sus clases, actividades, avisos de clase y recordatorios personales.
- `STUDENT`: se une a clases mediante un código y crea recordatorios personales. No crea clases ni avisos de clase.

No existe un tercer rol funcional ni un superadministrador global.

## Requisitos funcionales

| ID | Requisito | Criterio de aceptación resumido | Evidencia técnica |
|---|---|---|---|
| RF-01 | Registro y autenticación | Estudiante y profesor pueden elegir su rol, registrarse, confirmar correo, iniciar sesión y recuperar contraseña mediante Cognito. | `frontend/src/api/client.ts`, selector de rol y Lambda de post-confirmación |
| RF-02 | Autorización por rol | Cognito y el backend reconocen solo un grupo exacto `ADMIN` o `STUDENT`; grupo ausente, legacy, desconocido o ambiguo y una operación no permitida responden 403. | Configuración de seguridad y pruebas de grupos/seguridad |
| RF-03 | Perfil | El usuario consulta y actualiza su propio perfil; no puede editar perfiles ajenos. | `UserController`, `UserService` |
| RF-04 | Gestión de clases | El profesor crea, consulta y elimina sus clases. Otro profesor no puede administrarlas. | `CourseController`, `CourseService` |
| RF-05 | Curso no duplicado | Un profesor no puede crear dos cursos con el mismo nombre y descripción normalizados. La API responde 409 y la interfaz avisa antes de enviar. | Índice único funcional, servicio y formulario de curso |
| RF-06 | Unión a clase | El estudiante se une con un código válido; un profesor no puede unirse y una unión repetida se rechaza. | `POST /academic-reminder/courses/join` |
| RF-07 | Recordatorios personales | Ambos roles crean, editan, completan, deshacen una finalización accidental, priorizan y eliminan sus recordatorios personales. | `ReminderController`, pantallas `reminder/*` |
| RF-08 | Avisos de clase | Solo el profesor propietario crea recordatorios de una clase; los estudiantes inscritos los consultan, completan y reabren de forma individual. | `ReminderService`, estados por estudiante |
| RF-09 | Actividades | El profesor crea, edita y elimina actividades y consulta quién las completó y cuándo; el estudiante inscrito registra o deshace su finalización. | `ActivityController`, `ActivityService` |
| RF-10 | Numeración por profesor | El primer número visible de actividad de cada profesor es 1 y su secuencia es independiente de otros profesores. | `activity_counters` y UPSERT atómico |
| RF-11 | Historial y urgencia | Una pestaña reúne recordatorios y actividades completadas/vencidas; también se destacan las que vencen dentro de 24 horas. | Pestaña `history`, detalle de curso y utilidades de estado |
| RF-12 | Prioridades | Cada usuario administra categorías de prioridad propias y puede aplicarlas a sus recordatorios. | `PriorityCategoryController` |
| RF-13 | Avisos internos | El usuario configura anticipaciones y consulta próximos avisos desde el botón de campana. No se solicitan permisos ni se generan alarmas del sistema operativo. | `NotificationController`, pantalla `notifications` |
| RF-14 | Asistencia | El profesor registra asistencia solo para estudiantes inscritos y consulta un historial visual por nombre, fecha y estado. | `AttendanceController`, `AttendanceService`, pantalla `attendance` |
| RF-15 | Auditoría | Cambios relevantes de usuarios y del dominio académico quedan registrados con actor, entidad y fecha. | Tablas `audit_log`, `AuditService` |

## Matriz de permisos

| Operación | ADMIN (profesor) | STUDENT |
|---|:---:|:---:|
| Crear clase | Sí | No |
| Unirse a clase | No | Sí |
| Crear actividad de clase | Sí, solo en clase propia | No |
| Editar/eliminar actividad | Sí, solo si es propietario | No |
| Completar actividad | No | Sí, si está inscrito |
| Deshacer actividad completada | No | Sí, únicamente su propio registro |
| Ver quién completó una actividad | Sí, solo en clase propia | No; solo ve su propio estado |
| Registrar asistencia | Sí, solo en clase propia | No |
| Crear aviso de clase | Sí, solo en clase propia | No |
| Crear recordatorio personal | Sí | Sí |
| Deshacer recordatorio personal completado | Sí | Sí |
| Administrar prioridades propias | Sí | Sí |
| Consultar datos ajenos | No | No |

## Requisitos no funcionales

| ID | Requisito | Verificación |
|---|---|---|
| RNF-01 Seguridad | JWT de Cognito, autorización en controlador y servicio, RDS privado, secretos fuera de Git y HTTPS público. | Pruebas de seguridad, CloudFormation y Postman |
| RNF-02 Portabilidad | El backend se construye y ejecuta con Docker Compose sin PostgreSQL, frontend ni pgAdmin locales en producción. | `compose.production.yml` |
| RNF-03 Integridad | Restricciones SQL, transacciones, índices únicos y validación de DTO evitan estados inválidos y duplicados. | `database/init.sql`, `upgrade.sql`, DTO y servicios |
| RNF-04 Errores | 400, 401, 403, 404 y 409 se usan para fallos esperados; los errores tienen un cuerpo uniforme. | Manejadores globales y colección Postman |
| RNF-05 Observabilidad | Salud, logs de contenedores, correlación de solicitudes y retención de 30 días en CloudWatch. | `/health/*`, filtros de logging y CloudFormation |
| RNF-06 Mantenibilidad | Capas controller/service/repository, mappers, excepciones propias, pruebas y ADR. | Código Kotlin y documentación |
| RNF-07 Usabilidad | Acciones con texto, validación inmediata, estados vacíos, urgencia visible y navegación diferenciada por rol. | Frontend Expo/React Native |
| RNF-08 Rendimiento | Las consultas principales usan índices; la corrida AWS obtuvo 167 ms de promedio, sin usar ese valor como SLA. | Reporte Newman del 10-08-2026 |
| RNF-09 Respaldo | RDS tiene cifrado y respaldo automático; el bucket de artefactos es privado, cifrado y versionado. | Stack `daily-reminder-production` |

## Casos de uso de aceptación

1. Un estudiante se registra, confirma el correo, inicia sesión y crea un recordatorio personal.
2. Un profesor elige “Profesor” al registrarse, queda únicamente en `ADMIN`, su perfil se sincroniza en `users_db` y puede crear una clase.
3. Intentar repetir el nombre y la descripción normalizados de una clase del mismo profesor produce 409.
4. Un estudiante se une con el código; otro profesor recibe 403 si intenta unirse.
5. El profesor crea y edita una actividad; el estudiante la completa, la ve en historial y puede deshacerla. El profesor ve nombre y fecha de la entrega.
6. Dos profesores crean su primera actividad y ambos ven el número 1.
7. Una actividad a menos de 24 horas se muestra como urgente; una vencida se diferencia visualmente.
8. El profesor registra asistencia solo para un estudiante inscrito.
9. Un token ausente o inválido produce 401; acceso fuera del rol produce 403; un recurso ajeno no queda expuesto.
10. Fecha y hora se eligen con calendario/reloj tanto en Android como en web.

## Trazabilidad con Jira

Los ID RF/RNF de este documento son la referencia estable del repositorio. Las claves reales de Jira deben vincularse sin inventarlas. El archivo [`jira-handoff.md`](jira-handoff.md) contiene la lista exacta de historias y evidencias que deben existir antes de la entrega.
