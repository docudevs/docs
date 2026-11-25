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

SCHEMA='{
  "type": "object",
  "properties": {
    "scene_summary": {"type": "string"},
    "objects": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {"type": "string", "description": "Short human-readable name, such as ball or player in red jersey"},
          "object_type": {"type": "string", "description": "Category such as player, ball, goal, field_marking, or equipment"},
          "description": {"type": "string", "description": "Brief visual description that helps identify the boxed object"},
          "bbox": {
            "type": "object",
            "description": "Normalized bounding box in image coordinates",
            "properties": {
              "left": {"type": "number", "minimum": 0, "maximum": 1},
              "top": {"type": "number", "minimum": 0, "maximum": 1},
              "right": {"type": "number", "minimum": 0, "maximum": 1},
              "bottom": {"type": "number", "minimum": 0, "maximum": 1}
            },
            "required": ["left", "top", "right", "bottom"]
          },
          "confidence": {"type": "number", "minimum": 0, "maximum": 1}
        },
        "required": ["name", "object_type", "description", "bbox", "confidence"]
      }
    }
  },
  "required": ["scene_summary", "objects"]
}'

PROMPT='Detect the main visible objects in this soccer image.

Return visible objects only. Include soccer players, the ball, and any prominent
sports equipment or field markings that are clearly visible. Do not invent
objects that are hidden, implied, or outside the frame.

For each object:
- Use a concise source-image label in name.
- Set object_type to a short category such as player, ball, goal, field_marking, or equipment.
- Set bbox to the visible extent of the object only, not a group or surrounding whitespace.
- Use normalized coordinates from 0 to 1 in image space with origin at the top-left.
- Set confidence from 0 to 1.

Return JSON only. The JSON must match the provided schema.'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@soccer.jpg" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  --arg prompt "$PROMPT" \
  '{
    mimeType: "image/jpeg",
    prompt: $prompt,
    schema: $schema,
    ocr: "PREMIUM",
    llm: "DEFAULT"
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
