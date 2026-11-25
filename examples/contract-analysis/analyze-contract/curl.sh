#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
KNOWLEDGE_BASE_ID=42  # id of the case whose profile is published

wait_for_analysis() {
  for _ in $(seq 60); do
    OPERATION=$(curl -s -S -f "https://api.docudevs.ai/operation/$1/contract-analysis" \
      -H "Authorization: $API_KEY")
    if [ "$(jq -r '.resultAvailable' <<< "$OPERATION")" = "true" ]; then return 0; fi
    case "$(jq -r '.status' <<< "$OPERATION")" in
      ERROR | FAILED | TIMEOUT) echo "Contract analysis for $1 failed" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Contract analysis for $1 did not finish in time" >&2
  return 1
}

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "'"$JOB_GUID"'",
    "type": "contract-analysis",
    "parameters": {"customParameters": {"knowledgeBaseId": '"$KNOWLEDGE_BASE_ID"'}}
  }'

wait_for_analysis "$JOB_GUID"
jq '.result | fromjson' <<< "$OPERATION"
# docs:end
