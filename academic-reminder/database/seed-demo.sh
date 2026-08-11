#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=professor_sub="$DEMO_PROFESSOR_SUB" \
  --set=student_sub="$DEMO_STUDENT_SUB" <<'SQL'
INSERT INTO priority_categories (owner_user_id, name, color, sort_order) VALUES
(:'student_sub', 'Critical', '#D92D20', 0),
(:'student_sub', 'Selected', '#7A5AF8', 1),
(:'student_sub', 'Low impact', '#16A34A', 2)
ON CONFLICT (owner_user_id, name) DO NOTHING;

INSERT INTO courses (name, description, join_code, professor_user_id) VALUES
('Software Architecture', 'Architecture decisions and documentation', 'ARQ-2026', :'professor_sub'),
('Database Systems', 'Relational modeling and PostgreSQL', 'DBS-2026', :'professor_sub')
ON CONFLICT (join_code) DO NOTHING;

INSERT INTO course_memberships (course_id, student_user_id)
SELECT id, :'student_sub' FROM courses WHERE join_code IN ('ARQ-2026', 'DBS-2026')
ON CONFLICT (course_id, student_user_id) DO NOTHING;

INSERT INTO activities (course_id, title, description, due_at, created_by_user_id, activity_number)
SELECT id, seed.title, seed.description, CURRENT_TIMESTAMP + seed.due_in, :'professor_sub', seed.activity_number
FROM courses
JOIN (VALUES
    ('ARQ-2026', 'Final integrator project', 'Present the API, architecture and evidence', INTERVAL '5 days', 1),
    ('ARQ-2026', 'Architecture diagram', 'Explain service and data boundaries', INTERVAL '2 days', 2),
    ('DBS-2026', 'PostgreSQL workshop', 'Complete the query exercises', INTERVAL '3 days', 3)
) AS seed(join_code, title, description, due_in, activity_number) ON seed.join_code = courses.join_code
WHERE NOT EXISTS (SELECT 1 FROM activities existing WHERE existing.course_id = courses.id AND existing.title = seed.title);

INSERT INTO activity_counters (owner_user_id, last_value)
SELECT :'professor_sub', MAX(activity_number)
FROM activities
WHERE created_by_user_id = :'professor_sub'
HAVING MAX(activity_number) IS NOT NULL
ON CONFLICT (owner_user_id) DO UPDATE
SET last_value = GREATEST(activity_counters.last_value, EXCLUDED.last_value);

INSERT INTO reminders (course_id, created_by_user_id, title, description, type, due_at, priority)
SELECT courses.id, :'professor_sub', seed.title, seed.description, seed.type, CURRENT_TIMESTAMP + seed.due_in, seed.priority
FROM courses
JOIN (VALUES
    ('ARQ-2026', 'Submit architecture report', 'Upload the final repository link', 'PROJECT', INTERVAL '6 days', 'URGENT'),
    ('DBS-2026', 'Database exam', 'Review normalization and SQL', 'EXAM', INTERVAL '9 days', 'HIGH')
) AS seed(join_code, title, description, type, due_in, priority) ON seed.join_code = courses.join_code
WHERE NOT EXISTS (SELECT 1 FROM reminders existing WHERE existing.course_id = courses.id AND existing.title = seed.title);

INSERT INTO reminders (owner_user_id, created_by_user_id, title, description, type, due_at, priority, priority_category_id)
SELECT :'student_sub', :'student_sub', seed.title, seed.description, seed.type, CURRENT_TIMESTAMP + seed.due_in, seed.priority,
       CASE WHEN seed.category_name IS NULL THEN NULL ELSE categories.id END
FROM (VALUES
    ('Prepare architecture presentation', 'Review the slides', 'PRESENTATION', INTERVAL '7 days', 'HIGH', 'Critical'),
    ('Read database chapter', 'Prepare questions for class', 'TASK', INTERVAL '4 days', 'MEDIUM', 'Selected')
) AS seed(title, description, type, due_in, priority, category_name)
LEFT JOIN priority_categories categories ON categories.owner_user_id = :'student_sub' AND categories.name = seed.category_name
WHERE NOT EXISTS (SELECT 1 FROM reminders existing WHERE existing.owner_user_id = :'student_sub' AND existing.title = seed.title);

INSERT INTO activity_completions (activity_id, student_user_id)
SELECT activities.id, :'student_sub'
FROM activities
WHERE activities.title IN ('Architecture diagram', 'PostgreSQL workshop')
ON CONFLICT (activity_id, student_user_id) DO NOTHING;

INSERT INTO student_reminder_states (reminder_id, student_user_id, completed_at, priority_category_id)
SELECT reminders.id, :'student_sub',
       CASE WHEN reminders.title = 'Database exam' THEN CURRENT_TIMESTAMP ELSE NULL END,
       categories.id
FROM reminders
LEFT JOIN priority_categories categories ON categories.owner_user_id = :'student_sub' AND categories.name = 'Selected'
WHERE reminders.title IN ('Submit architecture report', 'Database exam')
ON CONFLICT (reminder_id, student_user_id) DO NOTHING;

INSERT INTO notifications (reminder_id, target_user_id, notify_at)
SELECT reminders.id, :'student_sub', reminders.due_at - offsets.value
FROM reminders
CROSS JOIN (VALUES (INTERVAL '1 hour'), (INTERVAL '1 day')) AS offsets(value)
WHERE (reminders.owner_user_id = :'student_sub' OR reminders.course_id IN (
    SELECT course_id FROM course_memberships WHERE student_user_id = :'student_sub'
)) AND reminders.due_at - offsets.value > CURRENT_TIMESTAMP
ON CONFLICT (reminder_id, target_user_id, notify_at) DO NOTHING;

INSERT INTO attendance (course_id, student_user_id, attendance_date, status, recorded_by_user_id)
SELECT courses.id, :'student_sub', dates.attendance_date, dates.status, :'professor_sub'
FROM courses
CROSS JOIN (VALUES
    (CURRENT_DATE - 1, 'PRESENT'),
    (CURRENT_DATE - 2, 'LATE')
) AS dates(attendance_date, status)
WHERE courses.join_code = 'ARQ-2026'
ON CONFLICT (course_id, student_user_id, attendance_date) DO NOTHING;

INSERT INTO audit_log (user_sub, action, entity_type, entity_id, new_values)
SELECT :'professor_sub', 'INSERT', 'Course', id::text, '{"source":"demo-seed"}' FROM courses
UNION ALL
SELECT :'student_sub', 'INSERT', 'Reminder', id::text, '{"source":"demo-seed"}' FROM reminders WHERE owner_user_id = :'student_sub';
SQL
