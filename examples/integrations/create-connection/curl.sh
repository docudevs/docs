#!/usr/bin/env bash
set -euo pipefail

# docs:start
# DB_PASSWORD holds the database password; it is sent in the write-only "secrets" map.
curl -s -S -f -X POST "https://api.docudevs.ai/integration/connections" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg password "$DB_PASSWORD" '{
    name: "warehouse-pg",
    connectorFamily: "postgres",
    active: true,
    fieldValues: {
      url: "jdbc:postgresql://db.example.com:5432/reporting",
      username: "svc"
    },
    secrets: {password: $password}
  }')"
# docs:end
