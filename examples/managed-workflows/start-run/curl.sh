#!/usr/bin/env bash
set -euo pipefail

# docs:start
# FLOW_CONFIG_ID is the number after "edit=" in the Edit link of the saved flow
# under Integrations > Flow Configs. It is not the flow name.
FLOW_CONFIG_ID=12

curl -s -S -f -X POST "https://api.docudevs.ai/managed-execution" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson id "$FLOW_CONFIG_ID" '{
    flowConfigId: $id,
    resourceClass: "small",
    timeoutSeconds: 600,
    inputs: {}
  }')" | jq '{executionId, status, resourceClass}'
# docs:end
