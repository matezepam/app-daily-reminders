# Modelo de negocio e innovación

## Business Model Canvas

| Bloque | Daily Reminder |
|---|---|
| Segmentos | Estudiantes, profesores, coordinaciones académicas e instituciones educativas pequeñas o medianas. |
| Propuesta de valor | Clases, actividades, asistencia y recordatorios en una sola experiencia móvil/web, con permisos simples y alertas de vencimiento. |
| Canales | Aplicación Android, web institucional, demostraciones académicas y alianzas con docentes. |
| Relación | Autoservicio para estudiantes, incorporación guiada para profesores y soporte institucional. |
| Ingresos | Freemium individual; licencia por profesor/curso; plan institucional por usuarios activos. |
| Recursos clave | Aplicación Expo, microservicios Kotlin, infraestructura AWS, Cognito y conocimiento del flujo académico. |
| Actividades clave | Desarrollo, seguridad, disponibilidad, soporte, mejora de UX y medición de adopción. |
| Socios clave | Institución educativa, docentes piloto, AWS, Expo y tiendas de aplicaciones. |
| Costos | Cómputo, base de datos, transferencia, correo/SMS de identidad, soporte y mantenimiento. |

## Innovación

La innovación no consiste solo en una lista de tareas. La aplicación combina contexto de clase y contexto personal sin mezclar propiedad: el profesor publica información compartida, mientras cada estudiante conserva estados de finalización y prioridades propias. La urgencia visual, el historial y la numeración por profesor reducen ambigüedad durante la conversación en clase.

## Plan financiero inicial

Supuestos de escenario piloto, no cotización contractual:

| Concepto mensual | Piloto | Crecimiento |
|---|---:|---:|
| Infraestructura AWS | USD 25–60 | USD 120–350 |
| Observabilidad y respaldos | incluido–20 | USD 30–100 |
| Soporte/mantenimiento | 20 horas | 60 horas |
| Marketing/capacitación | USD 50 | USD 300 |

Ingresos propuestos:

- Plan gratuito: recordatorios personales y hasta dos clases.
- Profesor: USD 4,99/mes por hasta diez clases activas.
- Institución: desde USD 1 por usuario activo/mes, con mínimo negociado.

Punto de equilibrio simplificado del piloto: con USD 60 de infraestructura y USD 200 de mantenimiento imputado, se requieren aproximadamente 53 profesores a USD 4,99/mes. Antes de fijar precios deben medirse usuarios activos, costo por usuario, retención de 30 días y número de actividades completadas a tiempo.

## Riesgos y mitigación

| Riesgo | Mitigación |
|---|---|
| Baja adopción docente | Piloto con una materia, importación simple y demostración de 10 minutos. |
| Fatiga de notificaciones | Anticipaciones configurables y cancelables. |
| Exposición de datos | Mínimo privilegio, RDS privado, JWT y pruebas de aislamiento. |
| Costos variables | Alarmas presupuestarias, límites de retención y escalamiento por métricas. |
| Dependencia de proveedor | Docker, PostgreSQL estándar, IaC y contratos HTTP documentados. |
