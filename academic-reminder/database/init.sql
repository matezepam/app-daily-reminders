CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    join_code VARCHAR(20) NOT NULL UNIQUE,
    professor_user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_memberships (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_user_id VARCHAR(100) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_membership_course_student UNIQUE (course_id, student_user_id)
);

CREATE TABLE IF NOT EXISTS priority_categories (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id VARCHAR(100) NOT NULL,
    name VARCHAR(40) NOT NULL,
    color VARCHAR(7) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_priority_owner_name UNIQUE (owner_user_id, name)
);

CREATE TABLE IF NOT EXISTS reminders (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    owner_user_id VARCHAR(100),
    created_by_user_id VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(30) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    priority VARCHAR(20) NOT NULL,
    priority_category_id BIGINT REFERENCES priority_categories(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reminder_scope CHECK ((course_id IS NOT NULL AND owner_user_id IS NULL) OR (course_id IS NULL AND owner_user_id IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS student_reminder_states (
    id BIGSERIAL PRIMARY KEY,
    reminder_id BIGINT NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
    student_user_id VARCHAR(100) NOT NULL,
    completed_at TIMESTAMPTZ,
    priority_category_id BIGINT REFERENCES priority_categories(id) ON DELETE SET NULL,
    CONSTRAINT uk_state_reminder_student UNIQUE (reminder_id, student_user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    reminder_id BIGINT NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
    target_user_id VARCHAR(100) NOT NULL,
    notify_at TIMESTAMPTZ NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_target_time UNIQUE (reminder_id, target_user_id, notify_at)
);

CREATE TABLE IF NOT EXISTS activity_counters (
    owner_user_id VARCHAR(100) PRIMARY KEY,
    last_value BIGINT NOT NULL CHECK (last_value > 0)
);

CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    due_at TIMESTAMPTZ NOT NULL,
    created_by_user_id VARCHAR(100) NOT NULL,
    activity_number BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_activity_creator_number UNIQUE (created_by_user_id, activity_number)
);

CREATE TABLE IF NOT EXISTS activity_completions (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_user_id VARCHAR(100) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_activity_completion_student UNIQUE (activity_id, student_user_id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_user_id VARCHAR(100) NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_by_user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_course_student_date UNIQUE (course_id, student_user_id, attendance_date)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_sub VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    previous_values TEXT,
    new_values TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_courses_professor ON courses(professor_user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_student ON course_memberships(student_user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_course_due ON reminders(course_id, due_at);
CREATE INDEX IF NOT EXISTS idx_reminders_owner_due ON reminders(owner_user_id, due_at);
CREATE INDEX IF NOT EXISTS idx_notifications_target_time ON notifications(target_user_id, notify_at);
CREATE INDEX IF NOT EXISTS idx_activities_course_due ON activities(course_id, due_at);
CREATE INDEX IF NOT EXISTS idx_attendance_course_date ON attendance(course_id, attendance_date);
CREATE INDEX IF NOT EXISTS idx_academic_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_academic_audit_user ON audit_log(user_sub, created_at);
