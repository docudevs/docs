#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with sourceLocations enabled

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/source-locations" \
  -H "Authorization: $API_KEY" -o source-locations.json

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/layout?include=paragraphs" \
  -H "Authorization: $API_KEY" -o layout.json

# Print each pointer, its source reference and the paragraph text for that reference.
# References to tables or figures print an empty string.
jq -r --slurpfile layout layout.json '
  (($layout[0].paragraphs // []) | map({(.id): .content}) | add // {}) as $text
  | (.locations // {}) | to_entries[]
  | .key as $pointer
  | (.value.sourceRefs // [])[]
  | "\($pointer) \(.) \($text[.] // "")"
' source-locations.json
# docs:end
