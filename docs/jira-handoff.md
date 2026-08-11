# Traspaso y verificación de Jira

No se deben inventar claves ni marcar historias como terminadas sin revisar el tablero real. Antes de la entrega, Jira debe contener o vincular como mínimo estas historias:

| Historia necesaria | Criterios/evidencia que debe incluir |
|---|---|
| Autenticación Cognito | Registro seleccionable de estudiante/profesor, confirmación, Lambda post-confirmación, login, recuperación y solo dos roles. |
| Matriz ADMIN/STUDENT | Tabla de permisos y casos 401/403. |
| Gestión de cursos | Propiedad del profesor, código de unión y rechazo de duplicado normalizado 409. |
| Recordatorios personales | Creación por ambos roles, edición, prioridad, finalización e historial. |
| Avisos y actividades de clase | CRUD del profesor, consulta/finalización del estudiante y urgencia de 24 h. |
| Numeración de actividad | Secuencia visible independiente por profesor y prueba concurrente/atómica. |
| Asistencia | Solo inscritos y solo profesor propietario. |
| Backend y base externa | Docker, RDS privado, SQL sin Flyway, secretos e idempotencia. |
| Despliegue web | S3/CloudFront y API HTTPS sin localhost. |
| APK Android | Build EAS, instalación real y enlace AWS. |
| Calidad | 147 pruebas backend + 3 Lambda, cobertura, Newman 53/106, cero 500 y logs limpios. |
| Infraestructura Cognito como código | Corregir la plantilla legacy del stack para que una actualización futura no restaure `PROFESSOR`/`USER`; conservar la prueba y el despliegue de `cognito-registration`. |
| Documentación de defensa | RF/RNF, casos, ADR, arquitectura, escalamiento, Canvas y finanzas. |

## Correspondencia con el tablero SCRUM

Historias existentes que deben ampliarse con los criterios y evidencias de esta entrega:

- `SCRUM-94` / HU-03: registro con selector estudiante/profesor (`STUDENT`/`ADMIN`).
- `SCRUM-24` / HU-02: JWT, matriz de permisos y rechazo 401/403.
- `SCRUM-26` / HU-07: crear curso y rechazar con 409 nombre+descripción duplicados normalizados.
- `SCRUM-20` / HU-12: publicar y editar actividades, numeración visible independiente por profesor y urgencia menor a 24 horas.
- `SCRUM-22`, `SCRUM-28`, `SCRUM-31`, `SCRUM-30`, `SCRUM-32` y `SCRUM-34`: CRUD, listado, historial y categorías de recordatorios personales para ambos roles.
- `SCRUM-27` / HU-17: completar actividad sin perder el historial.
- `SCRUM-19` / HU-24: vencimiento automático y presentación visual del estado vencido.
- `SCRUM-97`, `SCRUM-98`, `SCRUM-99` y `SCRUM-100`: perfil, detalle del curso, asistencia y calendario.

Historias técnicas nuevas que conviene agregar:

- Migración final del backend a los microservicios `users` y `academic-reminder`, cada uno con su propia base PostgreSQL.
- Orquestación local Docker Compose con siete servicios persistentes visibles en Docker Desktop, dos trabajos de inicialización y logs/health checks.
- Gateway Nginx para frontend, usuarios y API académica sin dependencias de `localhost` en producción.
- Despliegue AWS con Cognito, API Gateway, EC2, RDS privado, S3/CloudFront, CloudWatch y secretos en SSM/Secrets Manager.
- Pipeline CI, prueba de 147 casos backend + 3 Lambda, cobertura JaCoCo, TypeScript/lint/export web y Newman con 53 solicitudes/106 aserciones/0 fallos/0 respuestas 500.
- Generación, firma, instalación y distribución del APK con Expo EAS y CloudFront.
- Documentación de arquitectura, ADR, RF/RNF, escalamiento, seguridad, Canvas, finanzas y evidencia de rúbrica.
- Migración controlada de Expo SDK 54 y dependencias transitivas como deuda técnica posterior.

Quedan deliberadamente pendientes y fuera de esta entrega:

- `SCRUM-25` / HU-22: alarma local del dispositivo.
- `SCRUM-29` / HU-21: la anticipación se almacena y se muestra, pero la notificación local debe completarse junto con HU-22.
- `SCRUM-35` / HU-26: integración Moodle.
- `SCRUM-36` / HU-23: Firebase.

## Mensaje listo para la compañera

> Hola. Ya existe una rama de integración final para Daily Reminder. Por favor amplía en Jira SCRUM-94 con el registro estudiante/profesor y Lambda Cognito de solo dos grupos; SCRUM-24 con la matriz ADMIN/STUDENT y casos 401/403; SCRUM-26 con el rechazo 409 de cursos que repitan nombre+descripción; SCRUM-20 con edición, historial, numeración por profesor y urgencia de actividades; y SCRUM-22/28/31/30/32/34 con el CRUD e historial de recordatorios para ambos roles. También necesitamos historias técnicas para los dos microservicios y sus bases PostgreSQL, Docker Compose con siete servicios y logs, Nginx, despliegue AWS, CI/pruebas/Postman, APK y documentación de rúbrica. SCRUM-25 (alarma local), SCRUM-35 (Moodle) y SCRUM-36 (Firebase) siguen pendientes; SCRUM-29 queda parcial hasta hacer la alarma. Añade criterios de aceptación, puntos, responsable, sprint y enlace al Pull Request/evidencias; no pases a Done nada que aún no tenga validación.

## Definición de terminado

Una historia está terminada cuando tiene criterios de aceptación, código o configuración identificable, prueba automática o evidencia manual, revisión del flujo por rol y ausencia de secretos. El Pull Request de integración se vincula como evidencia, pero permanece sin fusionar hasta la validación funcional del equipo.
