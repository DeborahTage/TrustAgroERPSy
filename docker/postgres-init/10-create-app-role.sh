#!/bin/sh
set -eu

: "${APP_DB_USERNAME:?APP_DB_USERNAME is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

if [ "$APP_DB_USERNAME" = "$POSTGRES_USER" ] || [ "$APP_DB_PASSWORD" = "$POSTGRES_PASSWORD" ]; then
    echo "The application role must use a distinct username and password from the bootstrap account." >&2
    exit 1
fi

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 --quiet \
    --set=app_user="$APP_DB_USERNAME" \
    --set=app_password="$APP_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user') \gexec
SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'app_user') \gexec
SQL
