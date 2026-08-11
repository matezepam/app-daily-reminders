# Guía de defensa y demostración

## Qué dejar abierto antes de comenzar

1. Docker Desktop en la vista **Containers**, con el proyecto expandido para mostrar los 7 servicios persistentes saludables.
2. Un terminal en la raíz del repositorio, preparado para ejecutar `docker compose ps -a` y `docker compose logs --since 10m users academic-reminder nginx`.
3. Navegador con la web pública, los dos endpoints `/health/*`, GitHub y el tablero de Jira en pestañas separadas.
4. Postman Desktop con la colección `Academic Reminder API` y el ambiente `Academic Reminder AWS` seleccionado. Las credenciales se escriben solo en variables locales.
5. Una sesión ADMIN y otra STUDENT ya confirmadas, además de un curso de demostración cuyo código pueda compartir el profesor.
6. Un teléfono Android con el APK instalado y acceso por datos móviles, para demostrar que Docker Desktop y Metro no son necesarios.
7. `docs/testing-evidence.md` abierto como respaldo si la red falla.

No mostrar Secrets Manager, archivos `.env`, contraseñas, tokens ni la base RDS directamente.

## Guion de 12 minutos

1. Problema y propuesta (1 min): actividades dispersas, fechas perdidas y dificultad para distinguir responsabilidades.
2. Arquitectura (2 min): Expo, Cognito, API Gateway, Nginx, dos microservicios, RDS privado y CloudWatch.
3. Profesor/ADMIN (2 min): iniciar sesión, crear clase, demostrar rechazo de duplicado, crear/editar actividad, ver quién la completó y registrar asistencia.
4. Estudiante (2 min): unirse por código, crear recordatorio personal, completar/deshacer actividad y revisar historial/urgencia.
5. Calidad (2 min): 166 pruebas de backend + 3 de registro Cognito, 100% de líneas en Kotlin, colección Postman/Newman y códigos negativos correctos sin 500.
6. Nube y seguridad (2 min): roles, secretos, HTTPS, RDS privado, Docker, respaldos y escalamiento.
7. Negocio y cierre (1 min): Canvas, costos, modelo de ingreso y siguiente experimento.

## Orden seguro de la demostración

- Abrir primero los dos endpoints `/health/*` y la web pública.
- Mostrar que el registro permite escoger Profesor o Estudiante; para el resto de la demo usar una cuenta ADMIN y una STUDENT ya confirmadas.
- Crear datos con un sufijo de fecha para evitar colisiones involuntarias.
- Mostrar el 409 de curso duplicado como comportamiento correcto, no como falla.
- Mostrar que `id` es global y `activityNumber` comienza en 1 para una cuenta de profesor nueva.
- Completar una actividad como estudiante, comprobar nombre/fecha como profesor y luego deshacerla.
- Mostrar el 403 de una operación prohibida mediante Postman.
- No abrir Secrets Manager ni archivos `.env` durante la exposición.
- Conservar una captura del reporte de pruebas por si la red falla.

## Preguntas previsibles

**¿Por qué ADMIN no es superusuario?** Porque la rúbrica usa “administrador” como profesor. El sistema aplica propietario además de rol.

**¿Por qué no se reinicia el ID real?** Una clave primaria no debe reiniciarse por cuenta. Se agregó un número de negocio por profesor, atómico y seguro.

**¿Por qué existen respuestas 400/403/409?** Son rechazos esperados y precisos. Un backend maduro no convierte reglas de negocio en 500.

**¿Por qué dos microservicios?** Separa identidad/perfiles del dominio académico, permite evolución y pruebas independientes y mantiene contratos claros.

**¿Por qué no Flyway?** Es una decisión explícita del proyecto evaluado; se usa preparación SQL idempotente documentada.

**¿Cómo escalaría?** Primero vertical por costo; luego réplicas sin estado detrás de un balanceador, tareas programadas coordinadas y RDS Multi-AZ/read replicas.

## Lista de comprobación previa

- [x] La web 1.1.0 en CloudFront y ambos health checks responden 200.
- [x] Cognito opera únicamente con los grupos `ADMIN` y `STUDENT`.
- [x] Los datos sintéticos usados para la validación AWS fueron eliminados.
- [x] Postman usa la URL AWS y variables, nunca tokens pegados en la colección.
- [ ] APK instala y abre en un teléfono real.
- [ ] El APK inicia sesión y consulta el dashboard con datos móviles, sin red local.
- [ ] Historias de Jira tienen criterios, evidencia y estado correctos.
- [ ] Repositorio y rama final se deciden solo después de la aprobación del usuario.
