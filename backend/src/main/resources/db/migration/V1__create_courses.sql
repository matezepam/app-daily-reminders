CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    join_code VARCHAR(9) NOT NULL UNIQUE,
    owner_user_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courses_owner_user_id ON courses (owner_user_id);
