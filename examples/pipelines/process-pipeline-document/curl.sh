#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "ocr": "AUTO",
  "extractionMode": "PIPELINE",
  "trace": true,
  "pipeline": {
    "version": "2026-05",
    "ocr": {"mode": "AUTO", "sourceLocations": true, "qualityArtifact": true},
    "nodes": [
      {
        "id": "extract_data",
        "type": "extract",
        "source": "$ocr.content",
        "prompt": "Extract the requested fields.",
        "schema": {
          "type": "object",
          "properties": {"name": {"type": "string"}}
        },
        "sourceLocations": true
      },
      {
        "id": "final_result",
        "type": "final",
        "dependsOn": ["extract_data"],
        "output": "$nodes.extract_data.result"
      }
    ],
    "finals": [{"node": "final_result"}]
  }
}
JSON

wait_for_job "$GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/source-locations" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/status/$GUID" \
  -H "Authorization: $API_KEY" | jq '.pipeline.nodes'
# docs:end
