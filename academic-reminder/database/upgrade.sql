-- Idempotent schema upgrades for existing PostgreSQL volumes.
-- The evaluated project intentionally uses SQL scripts instead of Flyway.

BEGIN;

-- Preserve legacy duplicates while making their names distinguishable. The
-- unique join code keeps the automatic suffix deterministic and traceable.
WITH ranked_courses AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY
                professor_user_id,
                LOWER(BTRIM(name)),
                LOWER(BTRIM(COALESCE(description, '')))
            ORDER BY created_at, id
        ) AS duplicate_position
    FROM courses
),
legacy_duplicates AS (
    SELECT
        courses.id,
        ' [anterior ' || courses.join_code || ']' AS suffix
    FROM courses
    JOIN ranked_courses ON ranked_courses.id = courses.id
    WHERE ranked_courses.duplicate_position > 1
)
UPDATE courses
SET name = LEFT(BTRIM(courses.name), GREATEST(1, 100 - CHAR_LENGTH(legacy_duplicates.suffix))) || legacy_duplicates.suffix
FROM legacy_duplicates
WHERE courses.id = legacy_duplicates.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_courses_professor_name_description
    ON courses (
        professor_user_id,
        LOWER(BTRIM(name)),
        LOWER(BTRIM(COALESCE(description, '')))
    );

CREATE TABLE IF NOT EXISTS activity_counters (
    owner_user_id VARCHAR(100) PRIMARY KEY,
    last_value BIGINT NOT NULL CHECK (last_value > 0)
);

ALTER TABLE activities ADD COLUMN IF NOT EXISTS activity_number BIGINT;

WITH numbered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY created_by_user_id
            ORDER BY created_at, id
        ) AS activity_number
    FROM activities
    WHERE activity_number IS NULL
)
UPDATE activities
SET activity_number = numbered.activity_number
FROM numbered
WHERE activities.id = numbered.id;

ALTER TABLE activities ALTER COLUMN activity_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_activity_creator_number
    ON activities (created_by_user_id, activity_number);

INSERT INTO activity_counters (owner_user_id, last_value)
SELECT created_by_user_id, MAX(activity_number)
FROM activities
GROUP BY created_by_user_id
ON CONFLICT (owner_user_id) DO UPDATE
SET last_value = GREATEST(activity_counters.last_value, EXCLUDED.last_value);

COMMIT;
