# Cumplimiento de la rúbrica P01

Esta matriz vincula cada criterio entregado por PUCE TEC con evidencia verificable del proyecto. No reemplaza la prueba del usuario ni permite marcar Jira como terminado sin revisar el tablero real.

| Criterio | Cobertura en Daily Reminder | Evidencia principal | Estado |
|---|---|---|---|
| 1.1 RF/RNF y casos de uso | 15 RF, 9 RNF, criterios de aceptación y matriz de permisos para los dos actores. | [`requirements-and-traceability.md`](requirements-and-traceability.md) | Completo |
| 1.2 GitFlow | Flujo, ramas, convenciones y Definition of Done documentados. Los commits se crearán únicamente después de la aceptación del usuario. | [`development-workflow.md`](development-workflow.md) | Documentado; ejecución pendiente de aprobación |
| 1.3 Pruebas unitarias | 147 pruebas de backend y 3 de la Lambda aprobadas, sin fallos; cobertura de líneas 100% en ambos servicios Kotlin. | [`testing-evidence.md`](testing-evidence.md), reportes JaCoCo | Completo |
| 1.4 Priorización y ADR | Decisiones de roles, SQL sin Flyway e identificadores/duplicados registradas con contexto y consecuencias. | [`adr/`](adr/) | Completo |
| 2.1 Objetivo y funciones | Clases, unión, recordatorios, actividades, historial, urgencia, prioridades y asistencia operativos por rol. | Aplicación web y APK de evaluación | Automatizado completo; prueba física pendiente |
| 2.2 API y base de datos | Aplicación consume API Gateway HTTPS; microservicios persisten en RDS PostgreSQL privado. | [`architecture.md`](architecture.md), colección Postman | Completo |
| 2.3 Validación y patrones | DTO validados, errores uniformes, separación por capas, formularios con validación inmediata y confirmaciones destructivas. | Backend, frontend y pruebas negativas | Completo |
| 2.4 Usabilidad | Acciones con texto, estados vacíos, navegación por rol, urgencia visible y edición de actividades/recordatorios. | Prueba visual autenticada en CloudFront | Completo |
| 3.1 Dominio y datos | Entidades y relaciones 1:N/N:M documentadas; identificadores globales y número visible por profesor separados. | [`architecture.md`](architecture.md) | Completo |
| 3.2 Organización en capas | Controller, DTO, mapper, service, access service, repository, entity y manejador global. | `users/src`, `academic-reminder/src` | Completo |
| 3.3 Negocio y errores | Propiedad, rol, inscripción, duplicados, transiciones y fechas validadas; 400/401/403/404/409 comprobados. | Servicios, manejadores y Newman | Completo |
| 3.4 Calidad Kotlin | Nombres expresivos, responsabilidades separadas, transacciones e idempotencia sin lógica de negocio en controladores. | Código y análisis de pruebas | Completo |
| 3.5 Pruebas unitarias/funcionales | Servicios, mappers, seguridad, validaciones y casos válidos/inválidos cubiertos con mocks y aserciones. | [`testing-evidence.md`](testing-evidence.md) | Completo |
| 3.6 Autenticación/autorización | Registro autónomo de ambos roles, Lambda post-confirmación, JWT Cognito, solo `ADMIN`/`STUDENT`, endpoints públicos mínimos y restricciones por rol/propietario. | Pruebas de seguridad, Postman, Lambda y Cognito | Completo |
| 4.1 Escalamiento | Ventajas, límites y ruta de migración vertical/horizontal explicadas. | Sección Escalamiento de [`architecture.md`](architecture.md) | Completo |
| 4.2 Virtualización/nube | Docker Compose productivo, Nginx, EC2, RDS, API Gateway, Cognito, S3, CloudFront y CloudWatch. | [`../infrastructure/aws/README.md`](../infrastructure/aws/README.md) | Completo |
| 4.3 Arquitectura cloud | Infraestructura reproducible, RDS no público, secretos externos, HTTPS, logs y respaldos. | CloudFormation y [`architecture.md`](architecture.md) | Completo |
| 5.1 Business Model Canvas | Nueve bloques del Canvas definidos para el producto. | [`business-model.md`](business-model.md) | Completo |
| 5.2 Propuesta tecnológica | Valor académico, separación de propiedad, urgencia e historial descritos como diferenciadores. | [`business-model.md`](business-model.md) | Completo |
| 5.3 Plan financiero | Escenarios de costo, ingresos, punto de equilibrio, métricas y riesgos. | [`business-model.md`](business-model.md) | Completo |
| 6.1–6.5 Sustentación | Propuesta de valor, guion técnico, preguntas difíciles, demostración y checklist preparados. | [`defense-guide.md`](defense-guide.md) | Preparado; depende de la exposición del equipo |

## Evidencia de aceptación restante

Antes de declarar la entrega totalmente cerrada faltan únicamente evidencias que requieren intervención del equipo:

1. Instalar el APK publicado en un teléfono Android real, iniciar sesión y ejecutar los casos de aceptación.
2. Revisar las claves y estados del tablero Jira con la compañera; no se deben inventar ni modificar sin acceso.
3. Después de la aprobación expresa del usuario, crear ramas/commits/PR siguiendo el GitFlow documentado.
