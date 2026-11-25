#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PORTAL_ID is the "id" returned when the portal was created or listed.
PORTAL_ID=42

# The response holds the receiptCode and the jobGuid
curl -s -S -f -X POST "https://api.docudevs.ai/submission-portals/$PORTAL_ID/submissions" \
  -H "Authorization: $API_KEY" \
  -F "documents=@invoice.pdf"
# docs:end
