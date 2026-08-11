# Estrategia de pruebas y evidencia

## Pirámide de pruebas

1. Unitarias: servicios, mappers, validadores JWT, auditoría y filtros.
2. Funcionales de capa web: delegación, seguridad, respuestas y manejadores de excepción.
3. Integración de contrato: colección Postman/Newman contra la URL HTTPS real.
4. Smoke test: salud de ambos microservicios, carga de CloudFront y ausencia de errores de consola.

## Resultado verificado el 11-08-2026

| Verificación | Resultado |
|---|---:|
| Pruebas `academic-reminder` | 127 aprobadas |
| Pruebas `users` | 39 aprobadas |
| Total backend | 166 aprobadas; 0 fallos; 0 omitidas |
| Lambda de registro Cognito | 3 aprobadas; `ADMIN`, valor inválido y recuperación |
| Total automatizado unitario | 169 aprobadas; 0 fallos |
| Cobertura de líneas JaCoCo | 100% en ambos módulos |
| Cobertura de ramas | 79,37% académica; 83,33% usuarios |
| Newman contra AWS | 59 solicitudes automatizadas; 118 aserciones; 0 fallos; 1 caso manual de resiliencia |
| HTTP 500 en Newman | 0 |
| Eventos `ERROR`/`Exception` en CloudWatch durante la corrida | 0 / 0 |
| Frontend | TypeScript, lint y export web aprobados |
| CloudFront | Web 1.1.0: `index.html` y `entry-3225db8c6a03a566aa9524845089ae02.js` HTTP 200 |
| Referencias de backend local en bundle público | 0 |
| APK Android | APK público 1.0.0 verificado; build EAS 1.1.0/versionCode 2 solicitado para aceptación final |

## Dependencias del frontend

La aplicación permanece en Expo SDK 54 porque la [guía oficial de creación de proyectos de Expo](https://docs.expo.dev/get-started/create-a-project/) recomienda esa versión durante la transición actual cuando se necesita Expo Go en un dispositivo físico. `expo-doctor` aprobó 18/18 comprobaciones.

`npm audit --omit=dev` informa 26 avisos transitivos de la cadena Expo/Metro (11 moderados y 15 altos, 0 críticos). La simulación de `npm audit fix` no ofrece cambios compatibles; la única propuesta global es un salto mayor a Expo SDK 57. No se aplicó `--force`, porque la [guía oficial de actualización](https://docs.expo.dev/workflow/upgrading-expo-sdk-walkthrough/) exige migraciones incrementales 54→55→56→57 y esa acción invalidaría la compatibilidad y las pruebas actuales. Estos paquetes se usan principalmente durante configuración/empaquetado y no corresponden a dependencias del backend desplegado. La migración deberá planificarse como una historia técnica separada después de la entrega y probarse con un nuevo APK.

Prueba visual autenticada adicional en CloudFront:

- login como ADMIN sintético;
- tarjetas explícitas “Nuevo recordatorio” y “Crear una clase”;
- creación de curso y bloqueo inmediato del duplicado;
- actividad visible como `Actividad #1`;
- estado “Por vencer” con cuenta regresiva menor a 24 horas;
- edición de título y descripción;
- confirmación web de eliminación y borrado exitoso;
- cierre de sesión, eliminación del usuario sintético y residuos en cero.

Prueba integral adicional del registro de profesor:

- el formulario envió `custom:role=ADMIN` a Cognito;
- la Lambda de post-confirmación dejó la cuenta únicamente en el grupo `ADMIN`;
- el permiso de invocación quedó restringido al ARN del User Pool y una nueva confirmación real terminó con `signup=0`, `confirm=0`, grupo `ADMIN` y cero usuarios residuales;
- el login mostró la navegación de profesor y la acción “Crear una clase”;
- `/users/me` creó el perfil `ADMIN` relacionado por el `sub` de Cognito en `users_db` y generó su auditoría;
- la cuenta y sus filas sintéticas se eliminaron después de comprobar que no quedaban residuos.

Distribución deliberada de estados de la corrida Newman:

```json
{
  "200": 31,
  "201": 7,
  "204": 8,
  "400": 1,
  "401": 1,
  "403": 2,
  "404": 1,
  "409": 2,
  "500": 0
}
```

Los 4xx forman parte de casos negativos explícitos: entrada inválida, falta de token, rol incorrecto, recurso inexistente y duplicados. Una API correcta debe rechazarlos con el código de negocio adecuado, no convertirlos en 500.

## Validación local de la versión 1.1.0 — 11-08-2026

| Verificación | Resultado |
|---|---:|
| Pruebas `academic-reminder` | 127 aprobadas; 0 fallos; 0 omitidas |
| Pruebas `users` | 39 aprobadas; 0 fallos; 0 omitidas |
| Total backend | 166 aprobadas |
| Lambda de registro Cognito | 3 aprobadas |
| Total automatizado backend/Lambda | 169 aprobadas |
| Cobertura de líneas JaCoCo | 100% en ambos módulos (`669/669` y `165/165`) |
| Cobertura de ramas | 79,37% académica; 83,33% usuarios |
| Frontend | TypeScript, lint y export web aprobados |
| Docker Desktop | 7 servicios persistentes saludables; 2 trabajos SQL terminaron con código 0 |
| Persistencia después de dos rebuilds | Conteos idénticos: 4 usuarios, 2 cursos, 3 actividades, 2 finalizaciones, 6 recordatorios y 2 asistencias |
| Logs posteriores al rebuild | 0 líneas `ERROR`/`FATAL`, 0 respuestas 500 |
| Colección Postman contra AWS | 59 solicitudes automáticas; 118 aserciones; 0 fallos; 0 respuestas 500 |
| Caso de resiliencia 503 | 1 solicitud manual, desactivada en la corrida normal para no interrumpir producción |
| Salud pública posterior | `users=UP`; `academic-reminder=UP` desde una computadora externa a AWS |
| CloudWatch posterior | 0 líneas `ERROR`/`FATAL`; 0 respuestas HTTP 500 en la ventana comprobada |
| Limpieza posterior a Postman | Cuentas Cognito y filas sintéticas eliminadas; datos reales preservados |

Prueba visual autenticada local de la versión 1.1.0:

- calendario y reloj web reales (`input type=date/time`), además del selector nativo Android;
- pestaña global Historial para actividades y recordatorios;
- recordatorio temporal completado con fecha exacta, reapertura exitosa y salida inmediata del historial;
- reproducción del conflicto de notificaciones al reabrir, corrección mediante `delete + flush + insert` transaccional y repetición sin error;
- dato temporal eliminado al finalizar la prueba;
- centro de avisos interno accesible desde la campana, sin permisos ni alarmas del sistema operativo.
- repetición de dos solicitudes simultáneas para deshacer un recordatorio: un `200`, un `409` controlado y cero `ERROR`/`5xx`; el backend serializa la transición con bloqueo pesimista y la interfaz deshabilita el botón durante la operación.
- recordatorio temporal, avisos y 5 filas de auditoría eliminados al terminar; residuos verificados en cero.

## Ejecución reproducible

```powershell
cd users
./gradlew.bat clean test jacocoTestReport

cd ../academic-reminder
./gradlew.bat clean test jacocoTestReport

cd ../frontend
npm ci
npm run typecheck
npm run lint
npx expo export --platform web --output-dir dist --clear

cd ../infrastructure/aws/cognito-registration
python -m unittest discover -v
```

Newman usa `postman/academic-reminder.postman_collection.json`. El ambiente de ejemplo no contiene contraseñas ni tokens. En una corrida real se deben inyectar `baseUrl`, región, App Client y credenciales temporales; después se eliminan los usuarios y datos sintéticos.

## Casos que siempre deben permanecer

- Matriz ADMIN/STUDENT completa, incluyendo 401 y 403.
- Token sin grupo, con rol legacy/desconocido o con ambos grupos produce 403; nunca obtiene `STUDENT` por defecto.
- Curso duplicado normalizado produce 409.
- Acceso cruzado entre profesores queda bloqueado.
- Actividad visible inicia en 1 para cada profesor.
- Edición y finalización conservan historial.
- Deshacer una finalización restaura avisos futuros sin crear duplicados.
- El profesor propietario recibe `completionCount` y la lista de entregas; el estudiante no recibe datos de compañeros.
- Fechas y campos inválidos producen 400 con `fieldErrors`.
- Recurso inexistente produce 404.
- Cada respuesta de la colección afirma explícitamente `status !== 500`.
- Curso, actividad y recordatorio muestran confirmación destructiva tanto en Android como en web.
