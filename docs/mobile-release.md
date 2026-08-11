# Entrega Android y Expo Go

## Identidad móvil

- Nombre: Daily Reminder
- Expo project: `@pmsalazare/daily-reminder`
- Project ID: `ba06f0ba-77ba-4c91-84f0-0837eeaf4141`
- Android package: `ec.edu.puce.dailyreminder`
- Versión: `1.0.0`
- Version code: `1`

## Build validado del 10-08-2026

- EAS build ID: `2ec24959-40e7-481a-b280-cd2d8a972934`
- Estado EAS: `FINISHED`
- Perfil: `production-apk`
- SDK: Expo 54 / Android compile y target SDK 36
- APK público: <https://d29ydq13vz3ryv.cloudfront.net/AcademicReminder.apk>
- Tamaño: `84.863.125` bytes
- SHA-256: `9b665727acdad93885fb152baff7c07e01a6b6489283ec2c16221af9c1abbe6a`
- Firma: APK Signature Scheme v2, RSA 2048; certificado SHA-256 `1722e22f73c20efc7a5c507dd0cd37a5397154523d482f35580f166b3d8a7a21`
- Paquete verificado: `ec.edu.puce.dailyreminder`, versión `1.0.0`, version code `1`

El bundle Android contiene la API pública y el App Client de Cognito de producción, y no contiene `localhost:9090` ni `10.0.2.2:9090`. El APK anterior se respaldó en el bucket privado de despliegue como `backups/android/AcademicReminder-before-build-2ec24959.apk` y se comparó por SHA-256 antes de reemplazarlo.

## APK instalable

El perfil `production-apk` de `frontend/eas.json` usa distribución interna y `android.buildType: apk`. Las variables `EXPO_PUBLIC_*` apuntan a API Gateway y Cognito de producción; no se necesita backend ni PostgreSQL local.

El APK no incorpora una base de datos. Solo lleva la interfaz, la URL pública de API Gateway y los identificadores públicos de Cognito. Después del login envía el JWT por HTTPS; Nginx dirige cada solicitud a `users` o `academic-reminder`, y esos contenedores son los únicos que leen o escriben en las bases privadas de RDS. Las credenciales de PostgreSQL permanecen en AWS y nunca se empaquetan en el teléfono.

```powershell
cd frontend
npx eas-cli@latest build --platform android --profile production-apk
```

Estado de comprobación:

1. [x] El build EAS termina en `FINISHED`.
2. [x] El artefacto es un ZIP/APK válido y tiene firma Android.
3. [x] El binario contiene el host de API Gateway y no contiene `localhost:9090` ni `10.0.2.2:9090`.
4. [x] Se guarda y verifica una copia del APK anterior.
5. [x] Se sube como `AcademicReminder.apk` con `application/vnd.android.package-archive`.
6. [x] CloudFront responde 200 y conserva el tamaño/hash esperado.
7. [ ] El usuario instala, inicia sesión y ejecuta el flujo de aceptación en un teléfono real.

## Prueba final en un teléfono

1. Descargar `AcademicReminder.apk` desde CloudFront y permitir la instalación desde el navegador si Android lo solicita.
2. Si Android rechaza actualizar una instalación muy antigua por diferencia de firma, desinstalar solo esa versión anterior e instalar el APK actual.
3. Abrir la app con Wi-Fi o datos móviles; Docker Desktop, Metro y PostgreSQL local deben permanecer apagados.
4. Crear una cuenta seleccionando **Profesor**, confirmar el código del correo e iniciar sesión. Deben aparecer “Crear una clase” y “Nuevo recordatorio”, pero no “Unirse a una clase”.
5. Crear una clase y una actividad; cerrar y volver a abrir la app para comprobar que los datos continúan en RDS.
6. Crear una segunda cuenta seleccionando **Estudiante**. Debe poder unirse con el código y crear recordatorios personales, pero no crear clases.
7. Probar edición, finalización, historial, aviso de menos de 24 horas y rechazo de una clase duplicada. Registrar capturas para Jira.

## Expo Go

Para desarrollo estándar:

```powershell
npm start
```

Para ejecutar Expo Go contra AWS, sin servicios locales:

```powershell
npm run start:aws
```

Metro sigue ejecutándose en la computadora porque Expo Go descarga el bundle de desarrollo. La autenticación, API y bases de datos de `start:aws` se encuentran en AWS.

## Publicación en tienda

El perfil `production` genera un Android App Bundle (`.aab`) para Google Play. El APK interno no se debe enviar a la tienda; sirve para instalación directa, evaluación y pruebas.
