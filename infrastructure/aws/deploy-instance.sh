#!/bin/bash
set -euo pipefail

: "${AWS_REGION:?AWS_REGION is required}"
: "${DATABASE_ENDPOINT:?DATABASE_ENDPOINT is required}"
: "${DATABASE_MASTER_SECRET_ARN:?DATABASE_MASTER_SECRET_ARN is required}"
: "${USERS_DATABASE_SECRET_ARN:?USERS_DATABASE_SECRET_ARN is required}"
: "${ACADEMIC_DATABASE_SECRET_ARN:?ACADEMIC_DATABASE_SECRET_ARN is required}"
: "${COGNITO_REGION:?COGNITO_REGION is required}"
: "${COGNITO_USER_POOL_ID:?COGNITO_USER_POOL_ID is required}"
: "${COGNITO_CLIENT_ID:?COGNITO_CLIENT_ID is required}"
: "${COGNITO_DOMAIN:?COGNITO_DOMAIN is required}"
: "${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS is required}"
: "${AWS_LOG_GROUP:?AWS_LOG_GROUP is required}"

APP_DIR=/opt/daily-reminder/source
cd "$APP_DIR"

master_secret=$(aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$DATABASE_MASTER_SECRET_ARN" --query SecretString --output text)
users_secret=$(aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$USERS_DATABASE_SECRET_ARN" --query SecretString --output text)
academic_secret=$(aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$ACADEMIC_DATABASE_SECRET_ARN" --query SecretString --output text)

master_user=$(jq -r '.username' <<<"$master_secret")
master_password=$(jq -r '.password' <<<"$master_secret")
users_user=$(jq -r '.username' <<<"$users_secret")
users_password=$(jq -r '.password' <<<"$users_secret")
academic_user=$(jq -r '.username' <<<"$academic_secret")
academic_password=$(jq -r '.password' <<<"$academic_secret")

for value in "$master_user" "$master_password" "$users_user" "$users_password" "$academic_user" "$academic_password"; do
  test -n "$value"
  test "$value" != "null"
done

export PGPASSWORD="$master_password"
for attempt in $(seq 1 60); do
  if pg_isready --host "$DATABASE_ENDPOINT" --port 5432 --username "$master_user" --dbname postgres >/dev/null 2>&1; then
    break
  fi
  sleep 10
done
pg_isready --host "$DATABASE_ENDPOINT" --port 5432 --username "$master_user" --dbname postgres

psql \
  --host "$DATABASE_ENDPOINT" \
  --port 5432 \
  --username "$master_user" \
  --dbname postgres \
  --set ON_ERROR_STOP=1 \
  --set master_user="$master_user" \
  --set users_user="$users_user" \
  --set users_password="$users_password" \
  --set academic_user="$academic_user" \
  --set academic_password="$academic_password" \
  --set users_database=users_db \
  --set academic_database=academic_reminder_db <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'users_user', :'users_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'users_user')
\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'users_user', :'users_password')
\gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'academic_user', :'academic_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'academic_user')
\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'academic_user', :'academic_password')
\gexec
-- PostgreSQL 18 requires the session user to be allowed to SET ROLE to a
-- different database owner. RDS master users do not bypass that check.
SELECT format('GRANT %I TO %I', :'users_user', :'master_user')
\gexec
SELECT format('GRANT %I TO %I', :'academic_user', :'master_user')
\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'users_database', :'users_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'users_database')
\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'academic_database', :'academic_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'academic_database')
\gexec
SQL
unset PGPASSWORD master_password master_secret users_secret academic_secret

umask 077
cat > .env.production <<EOF
BACKEND_HOST_PORT=80
AWS_REGION=$AWS_REGION
AWS_LOG_GROUP=$AWS_LOG_GROUP
COGNITO_REGION=$COGNITO_REGION
COGNITO_USER_POOL_ID=$COGNITO_USER_POOL_ID
COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID
COGNITO_ISSUER_URI=https://cognito-idp.$COGNITO_REGION.amazonaws.com/$COGNITO_USER_POOL_ID
COGNITO_DOMAIN=$COGNITO_DOMAIN
CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS
USERS_DB_HOST=$DATABASE_ENDPOINT
USERS_DB_PORT=5432
USERS_DB_NAME=users_db
USERS_DB_USER=$users_user
USERS_DB_PASSWORD=$users_password
ACADEMIC_DB_HOST=$DATABASE_ENDPOINT
ACADEMIC_DB_PORT=5432
ACADEMIC_DB_NAME=academic_reminder_db
ACADEMIC_DB_USER=$academic_user
ACADEMIC_DB_PASSWORD=$academic_password
DB_SSLMODE=require
USERS_SERVICE_TIMEOUT_MS=3000
EOF
chmod 0600 .env.production
unset users_password academic_password

export COMPOSE_PARALLEL_LIMIT=1
docker compose \
  --env-file .env.production \
  -f compose.production.yml \
  -f infrastructure/aws/compose.aws.yml \
  config --quiet
docker compose \
  --env-file .env.production \
  -f compose.production.yml \
  -f infrastructure/aws/compose.aws.yml \
  build users
docker compose \
  --env-file .env.production \
  -f compose.production.yml \
  -f infrastructure/aws/compose.aws.yml \
  build academic-reminder
docker compose \
  --env-file .env.production \
  -f compose.production.yml \
  -f infrastructure/aws/compose.aws.yml \
  up -d --no-build --wait

curl -fsS http://127.0.0.1/health/users >/dev/null
curl -fsS http://127.0.0.1/health/academic-reminder >/dev/null
date --iso-8601=seconds > /opt/daily-reminder/DEPLOYED
