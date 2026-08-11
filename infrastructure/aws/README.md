# Despliegue AWS

Esta infraestructura mantiene el frontend en el bucket y la distribución CloudFront existentes. El backend se ejecuta con Docker Compose en una instancia EC2 administrada mediante Systems Manager, usa una instancia PostgreSQL privada en RDS y se publica con una URL HTTPS de API Gateway.

## Recursos

- EC2 `t3.small`, sin puerto SSH y con disco cifrado.
- RDS PostgreSQL `db.t4g.micro`, cifrado, privado y con un día de respaldos automáticos (máximo permitido por el plan gratuito activo de esta cuenta).
- Tres secretos administrados por Secrets Manager; ninguna contraseña se guarda en Git.
- Bucket privado, cifrado y versionado para los artefactos del backend.
- API Gateway HTTP con URL HTTPS.
- Logs de los contenedores en `/daily-reminder/production/app` de CloudWatch Logs, con retención de 30 días.
- Los recursos de otros proyectos de la cuenta no se reutilizan ni modifican.

`cloudformation.yml` conserva el bucket y los secretos si se elimina el stack; RDS genera una instantánea final. `deploy-instance.sh` crea de forma idempotente las dos bases y sus usuarios, ejecuta los SQL `init.sql` y `upgrade.sql` sin Flyway y vuelve a construir los contenedores.

El artefacto esperado por defecto es `releases/current.zip` dentro del bucket creado por el stack. La URL de salida `BackendApiUrl` debe usarse como `EXPO_PUBLIC_API_URL` al construir el frontend.

## Despliegue activo

- Frontend: <https://d29ydq13vz3ryv.cloudfront.net>
- Portal de descarga: <https://d29ydq13vz3ryv.cloudfront.net/download/index.html>
- APK Android 1.1.0: <https://d29ydq13vz3ryv.cloudfront.net/download/AcademicReminder.apk>
- API: <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com>
- Salud usuarios: <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com/health/users>
- Salud académica: <https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com/health/academic-reminder>

La instancia se administra con Systems Manager y no expone SSH. RDS es privado; por tanto, ni Postman ni la aplicación se conectan directamente a PostgreSQL.

El portal de descarga se publica bajo el prefijo `download/` del bucket existente. `download/index.html` y `download/AcademicReminder.apk` quedan juntos para que los enlaces relativos funcionen, sin reemplazar la aplicación web de la raíz. El APK se sirve con `application/vnd.android.package-archive`, descarga como `AcademicReminder-1.1.0.apk` y conserva versión y SHA-256 en sus metadatos S3.

## Estado verificado — 11-08-2026

- Backend desplegado mediante el artefacto versionado `releases/current.zip` y AWS Systems Manager.
- Contenedores `users`, `academic-reminder` y `nginx` saludables; ambas tareas de preparación SQL finalizaron con código 0.
- Los dos endpoints de salud públicos respondieron `UP` después del despliegue.
- La web 1.1.0 se respaldó en `backups/web/20260811T194121Z/`, se publicó sin `--delete` y la invalidación CloudFront `IE1G21OQ6O6ZQ3K8RP6YSW40GO` terminó correctamente.
- El APK público anterior quedó intacto durante la publicación web: 84.863.125 bytes, ETag `7b630c23d47e973ca621d0cf92431bcd`.
- Una consulta exacta de CloudWatch posterior al despliegue no encontró eventos `ERROR`, excepciones ni respuestas HTTP 500.
- Newman ejecutó 59 solicitudes automatizadas y 118 aserciones contra API Gateway, con 0 fallos y 0 respuestas 500.
- Los datos sintéticos de la validación se eliminaron; los datos reales permanecen en RDS y no dependen de Docker Desktop.

Postman se ejecuta desde cualquier computadora importando `postman/academic-reminder.postman_collection.json` y `postman/academic-reminder-aws.postman_environment.json`. El ambiente contiene solo URLs e identificadores públicos; usuario, contraseña y tokens se completan localmente y nunca se guardan en Git.

## Registro Cognito de los dos roles

El formulario envía `custom:role=ADMIN` para profesor o `custom:role=STUDENT` para estudiante. La función de post-confirmación de [`cognito-registration/handler.py`](cognito-registration/handler.py) valida ese atributo, asigna exactamente uno de los dos grupos y elimina el contrario. Un valor ausente o inválido usa `STUDENT`; una recuperación de contraseña no cambia el rol.

El permiso `lambda:InvokeFunction` está limitado mediante `SourceArn` al User Pool de Daily Reminder, y la función solo puede agregar o quitar grupos dentro de ese mismo pool.

La configuración reproducible y sus pruebas están en `cognito-registration/`:

```bash
cd infrastructure/aws/cognito-registration
python -m unittest discover -v
./deploy.sh us-east-1 <USER_POOL_ID>
```

La cuenta AWS actual conserva una plantilla CloudFormation antigua que todavía nombra los grupos legacy `PROFESSOR` y `USER`, aunque el User Pool activo ya opera correctamente con `ADMIN` y `STUDENT`. Antes de una futura actualización de ese stack se debe reconciliar la plantilla o ejecutar de nuevo `deploy.sh`; de lo contrario, CloudFormation podría restaurar el comportamiento antiguo. Esta deuda de infraestructura está registrada en el traspaso de Jira y no afecta al flujo desplegado verificado.

## Cómo llega el APK a la base de datos

El APK solo contiene la interfaz, la URL HTTPS pública y los identificadores públicos de Cognito. No incluye PostgreSQL, credenciales de RDS ni una copia de los datos. Cognito autentica y entrega el JWT; el APK lo envía a API Gateway, Nginx enruta la solicitud al microservicio correspondiente y solo el microservicio accede a RDS con secretos administrados fuera del binario. Así, el mismo APK funciona desde cualquier red y teléfono con Internet, sin depender de Docker Desktop ni de una base instalada localmente.
