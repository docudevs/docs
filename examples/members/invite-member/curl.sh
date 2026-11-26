#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Answers 201 with the new member in status INVITED. No email is sent.
curl -s -S -f -X POST "https://api.docudevs.ai/members" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "new.colleague@example.com",
    "role": "MEMBER"
  }'
# docs:end
