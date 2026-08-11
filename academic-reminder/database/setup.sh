#!/bin/sh
set -eu

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

export PGPASSWORD="$DB_PASSWORD"
for script in /opt/daily-reminder/init.sql /opt/daily-reminder/upgrade.sql; do
  psql \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --dbname "$DB_NAME" \
    --set ON_ERROR_STOP=1 \
    --file "$script"
done
