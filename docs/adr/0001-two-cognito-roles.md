# ADR-0001: Dos roles de Cognito

- Estado: aceptado
- Fecha: 2026-08-09

## Contexto

La evaluación permite dos roles: estudiante y administrador. En el dominio, “administrador” representa al profesor, no a un operador con acceso global. Mantener `PROFESSOR`, `USER` o un tercer rol generaba ambigüedad y permisos inconsistentes.

## Decisión

Usar únicamente los grupos `ADMIN` y `STUDENT` de Cognito. `ADMIN` puede crear clases, actividades, asistencia y avisos solo dentro de sus propios cursos. Ambos roles pueden crear recordatorios personales. Solo `STUDENT` puede unirse a una clase.

El formulario de registro permite elegir `STUDENT` (estudiante) o `ADMIN` (profesor). El frontend envía la elección en el atributo `custom:role`; una función Lambda de post-confirmación valida el valor, agrega al usuario al grupo exacto y elimina el grupo contrario. Un valor ausente o inválido cae de forma segura en `STUDENT`. La recuperación de contraseña no modifica el rol.

## Consecuencias

- La matriz de permisos coincide en frontend, controladores y servicios.
- Un profesor no obtiene acceso global por llamarse `ADMIN`.
- Estudiantes y profesores pueden crear su cuenta de manera autónoma sin mantener grupos antiguos.
- Las migraciones normalizan roles antiguos antes de imponer la restricción SQL.
- Las pruebas negativas de propietario siguen siendo obligatorias.
