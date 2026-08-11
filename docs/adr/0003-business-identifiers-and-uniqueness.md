# ADR-0003: Identificadores de negocio y unicidad atómica

- Estado: aceptado
- Fecha: 2026-08-09

## Contexto

Los `BIGSERIAL` de PostgreSQL son globales y no deben reiniciarse por usuario. Sin embargo, la evaluación pide que el número mostrado de una actividad comience en 1 para cada profesor. También exige impedir cursos repetidos por nombre y descripción.

## Decisión

Conservar `activities.id` como clave primaria global y agregar `activity_number` como número visible. `activity_counters` incrementa por `owner_user_id` mediante un UPSERT atómico. La restricción única `(created_by_user_id, activity_number)` protege contra concurrencia.

Para cursos, aplicar unicidad por profesor sobre nombre y descripción normalizados con `lower`, `trim` y espacios colapsados. La capa de servicio detecta el caso para devolver un mensaje claro y el índice de PostgreSQL garantiza integridad final.

## Consecuencias

- No se manipulan secuencias globales ni se arriesgan claves foráneas.
- Dos profesores pueden ver actividad 1 sin compartir datos.
- Dos peticiones concurrentes no producen el mismo número visible.
- Un profesor puede reutilizar un título si cambia realmente la descripción; profesores distintos pueden usar el mismo material.
