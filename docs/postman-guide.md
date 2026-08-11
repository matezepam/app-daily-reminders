# Guía de consultas Postman

La colección ejecutable es `postman/academic-reminder.postman_collection.json`. Contiene 60 solicitudes organizadas de `00` a `08`; las carpetas `00`–`07` se ejecutan en orden y `08` es una prueba manual de indisponibilidad. Cada respuesta comprueba globalmente que el código no sea 500.

## Variables necesarias

Para AWS, importa `postman/academic-reminder-aws.postman_environment.json`. Ya contiene la URL pública de API Gateway, la región y el App Client público de Cognito; las credenciales y los tokens están vacíos. Para Docker Desktop puedes usar `postman/academic-reminder.postman_environment.json`.

Configura únicamente en tu ambiente local de Postman:

- `baseUrl`: ya viene como `https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com` en el ambiente AWS; usa `http://localhost:9090` solamente con el ambiente Docker Desktop.
- `cognitoRegion` y `cognitoClientId`.
- `professorUsername`, `professorPassword`, `studentUsername` y `studentPassword` de cuentas temporales de prueba.

No pegues contraseñas, JWT ni archivos `.env` en la colección o en Git. Las solicitudes de autenticación guardan automáticamente `professorAccessToken`, `studentAccessToken`, `professorSub` y `studentSub`. Las demás guardan los identificadores creados por la corrida.

La aplicación de Postman se ejecuta en tu computadora, pero al seleccionar **Academic Reminder - AWS** todas las solicitudes viajan por HTTPS a API Gateway. Postman nunca se conecta directamente a RDS y no necesita que Docker, Java o PostgreSQL estén encendidos localmente.

## Consultas principales

Todas las consultas del dominio usan `Authorization: Bearer {{professorAccessToken}}` o `Bearer {{studentAccessToken}}`.

| Objetivo | Método y ruta | Rol |
|---|---|---|
| Crear curso | `POST {{baseUrl}}/academic-reminder/courses` | ADMIN |
| Probar curso duplicado | Repetir el `POST` con nombre/descripción normalizados; espera 409 | ADMIN |
| Unirse a curso | `POST {{baseUrl}}/academic-reminder/courses/join` con `{"code":"{{courseCode}}"}` | STUDENT |
| Crear actividad | `POST {{baseUrl}}/academic-reminder/courses/{{courseId}}/activities` | ADMIN |
| Editar actividad | `PUT {{baseUrl}}/academic-reminder/activities/{{activityId}}` | ADMIN |
| Completar actividad | `PATCH {{baseUrl}}/academic-reminder/activities/{{activityId}}/complete` | STUDENT |
| Deshacer actividad | `DELETE {{baseUrl}}/academic-reminder/activities/{{activityId}}/completion` | STUDENT |
| Ver entregas | `GET {{baseUrl}}/academic-reminder/courses/{{courseId}}/activities` | ADMIN propietario |
| Completar recordatorio | `PATCH {{baseUrl}}/academic-reminder/reminders/{{personalReminderId}}/complete` | Propietario |
| Deshacer recordatorio | `DELETE {{baseUrl}}/academic-reminder/reminders/{{personalReminderId}}/completion` | Propietario |
| Próximos avisos | `GET {{baseUrl}}/academic-reminder/notifications/upcoming` | Ambos |
| Registrar asistencia | `PUT {{baseUrl}}/academic-reminder/courses/{{courseId}}/attendance` | ADMIN propietario |

Ejemplo para crear una actividad:

```json
{
  "title": "Defensa de arquitectura",
  "description": "Presentar servicios, seguridad y logs",
  "dueAt": "2035-08-16T18:00:00Z"
}
```

La lista de actividades devuelve dos identificadores distintos:

- `id`: clave primaria global e inmutable de la base de datos; no se reinicia y se usa en las URLs.
- `activityNumber`: número visible consecutivo e independiente por profesor; una cuenta nueva comienza en 1 aunque el `id` global sea 17 o 100.

Cuando consulta el profesor propietario, cada actividad incluye:

```json
{
  "completionCount": 1,
  "completions": [
    {
      "studentUserId": "sub-de-cognito",
      "completedAt": "2035-08-10T14:30:00Z"
    }
  ]
}
```

El nombre se obtiene una sola vez desde `GET /academic-reminder/courses/{{courseId}}/students` y la interfaz lo relaciona mediante `studentUserId`. Un estudiante no recibe el historial de otros estudiantes.

## Respuestas negativas esperadas

- 400: validación o JSON malformado.
- 401: token ausente o inválido.
- 403: rol, propietario o inscripción incorrectos.
- 404: recurso inexistente.
- 409: duplicado o transición repetida.
- 503: dependencia o base temporalmente indisponible.

Estos códigos son resultados controlados, no fallos del servidor. La carpeta `07 - Cleanup` elimina solamente los recursos cuyos ID fueron creados por la misma corrida.
