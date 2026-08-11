# ADR-0002: SQL idempotente sin Flyway

- Estado: aceptado
- Fecha: 2026-08-09

## Contexto

El backend evaluado originalmente no usa Flyway y el proyecto debe conservar ese criterio. A la vez, producción necesita crear bases nuevas y actualizar instalaciones existentes sin borrar datos.

## Decisión

Cada microservicio mantiene:

- `database/init.sql` para crear el esquema completo si no existe.
- `database/upgrade.sql` para transformaciones compatibles e idempotentes.
- `database/setup.sh` con `ON_ERROR_STOP` para ejecutar ambos archivos antes de iniciar la aplicación.

Docker Compose modela la preparación como servicios de una sola ejecución y hace que cada aplicación dependa de su finalización exitosa.

## Consecuencias

- No existe dependencia de Flyway.
- Repetir un despliegue no borra datos ni vuelve a aplicar cambios peligrosos.
- Todo cambio de esquema debe diseñarse explícitamente como idempotente y probarse tanto en una base vacía como en una existente.
- Las operaciones complejas requieren una copia de seguridad y una sección nueva y revisable en `upgrade.sql`.
