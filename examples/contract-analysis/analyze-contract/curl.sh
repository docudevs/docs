#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
KNOWLEDGE_BASE_ID=42  # id of the case whose profile is published

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "'"$JOB_GUID"'",
    "type": "contract-analysis",
    "parameters": {"customParameters": {"knowledgeBaseId": '"$KNOWLEDGE_BASE_ID"'}}
  }'

for _ in $(seq 60); do
  OPERATION=$(curl -s -S "https://api.docudevs.ai/operation/$JOB_GUID/contract-analysis" \
    -H "Authorization: $DOCUDEVS_API_KEY")
  if [ "$(jq -r '.resultAvailable' <<< "$OPERATION")" = "true" ]; then
    jq '.result | fromjson' <<< "$OPERATION"
    exit 0
  fi
  case "$(jq -r '.status' <<< "$OPERATION")" in
    ERROR | FAILED | TIMEOUT) echo "Contract analysis failed" >&2; exit 1 ;;
  esac
  sleep 2
done
echo "Contract analysis did not finish in time" >&2
exit 1
# docs:end
