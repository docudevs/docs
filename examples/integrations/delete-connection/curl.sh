#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CONNECTION_ID is the "id" returned when the connection was created or listed.
CONNECTION_ID=7

# 204 when deleted, 409 with the referencing flow configurations when the connection is in use
curl -s -S -X DELETE "https://api.docudevs.ai/integration/connections/$CONNECTION_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
# docs:end
