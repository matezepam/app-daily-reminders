CREATE TABLE course_memberships (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_user_id VARCHAR(128) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_membership_student UNIQUE (course_id, student_user_id)
);

CREATE INDEX idx_course_memberships_student ON course_memberships (student_user_id);
