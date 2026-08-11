#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=professor_sub="$DEMO_PROFESSOR_SUB" \
  --set=student_sub="$DEMO_STUDENT_SUB" <<'SQL'
INSERT INTO users (cognito_sub, email, full_name, role)
VALUES
    (:'professor_sub', 'profesor.demo@academicreminder.app', 'Demo Professor', 'ADMIN'),
    (:'student_sub', 'estudiante.demo@academicreminder.app', 'Demo Student', 'STUDENT')
ON CONFLICT (cognito_sub) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO audit_log (user_sub, action, entity_type, entity_id, new_values)
SELECT cognito_sub, 'INSERT', 'UserProfile', id::text, '{"source":"demo-seed"}'
FROM users
WHERE cognito_sub IN (:'professor_sub', :'student_sub');
SQL
