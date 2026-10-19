#!/bin/sh
# Arranque de la imagen. Algunas plataformas (p. ej. Render) entregan la base de datos como
# DATABASE_URL=postgresql://usuario:clave@host[:puerto]/base; Spring necesita una URL JDBC separada
# de las credenciales. Si DB_URL ya viene definida (docker compose) no se toca nada.
set -eu

if [ -n "${DATABASE_URL:-}" ] && [ -z "${DB_URL:-}" ]; then
  rest="${DATABASE_URL#*://}"
  creds="${rest%%@*}"
  location="${rest#*@}"
  export DB_USER="${creds%%:*}"
  export DB_PASSWORD="${creds#*:}"
  export DB_URL="jdbc:postgresql://${location}"
fi

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /app/app.jar
