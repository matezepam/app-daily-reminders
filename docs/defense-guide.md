# Guía de defensa y demostración

## Guion de 12 minutos

1. Problema y propuesta (1 min): actividades dispersas, fechas perdidas y dificultad para distinguir responsabilidades.
2. Arquitectura (2 min): Expo, Cognito, API Gateway, Nginx, dos microservicios, RDS privado y CloudWatch.
3. Profesor/ADMIN (2 min): iniciar sesión, crear clase, demostrar rechazo de duplicado, crear/editar actividad y registrar asistencia.
4. Estudiante (2 min): unirse por código, crear recordatorio personal, completar actividad y revisar historial/urgencia.
5. Calidad (2 min): 147 pruebas de backend + 3 de registro Cognito, 100% de líneas en Kotlin, colección Newman AWS y códigos negativos correctos sin 500.
6. Nube y seguridad (2 min): roles, secretos, HTTPS, RDS privado, Docker, respaldos y escalamiento.
7. Negocio y cierre (1 min): Canvas, costos, modelo de ingreso y siguiente experimento.

## Orden seguro de la demostración

- Abrir primero los dos endpoints `/health/*` y la web pública.
- Mostrar que el registro permite escoger Profesor o Estudiante; para el resto de la demo usar una cuenta ADMIN y una STUDENT ya confirmadas.
- Crear datos con un sufijo de fecha para evitar colisiones involuntarias.
- Mostrar el 409 de curso duplicado como comportamiento correcto, no como falla.
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

- [ ] CloudFront y ambos health checks responden 200.
- [ ] Cuenta ADMIN pertenece únicamente a `ADMIN` y cuenta estudiante únicamente a `STUDENT`.
- [ ] La clase y los datos de demo se pueden eliminar.
- [ ] Postman usa la URL AWS y variables, nunca tokens pegados en la colección.
- [ ] APK instala y abre en un teléfono real.
- [ ] El APK inicia sesión y consulta el dashboard con datos móviles, sin red local.
- [ ] Historias de Jira tienen criterios, evidencia y estado correctos.
- [ ] Repositorio y rama final se deciden solo después de la aprobación del usuario.
