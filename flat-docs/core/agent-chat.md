---
title: Agent Chat
description: Conversational AI agent for iterative document analysis with multi-turn chat sessions and document context.
sidebar_position: 4
---
<!-- Generated from docs/core/agent-chat.md. Do not edit by hand. -->

# Agent Chat

Agent Chat provides an interactive assistant that helps you design extraction configurations and answer document-related questions. It runs on a dedicated queue so chat requests stay responsive while document processing workloads are running.

## How It Works

1. Send a chat message with optional `sessionId` and `documentGuid`.
2. The API queues the request and returns a job GUID.
3. Poll the status endpoint until the response is complete.

## API Usage

**cURL**

```bash
wait_for_chat() {
  local deadline=$((SECONDS + 120))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR|FAILED) echo "Chat $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Chat $1 did not complete in time" >&2
  return 1
}

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/agent/chat" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "Help me build an invoice extraction schema"}
    ],
    "sessionId": "session-123"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '.response.message // empty'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # Sends the message, polls the status endpoint and raises TimeoutError after 120 seconds
    status = await client.agent_chat_and_wait(
        messages=[{"role": "user", "content": "Help me build an invoice extraction schema"}],
        session_id="session-123",
    )

    reply = status.get("response") or {}
    print(reply.get("message"))


asyncio.run(main())
```

The request starts a chat job and returns its `jobGuid` and the `sessionId`. Poll `GET /agent/status/{jobGuid}` until `status` is `COMPLETED`; the answer is in `response.message`. A job that ends in `ERROR` carries the reason in `error`. The cURL variant and the Python helper both give up after 120 seconds.

## Case-bound chat

Pass `caseId` to work on one case. The agent then follows the case's active memo instructions, works through case-bound tools and proposes changes that a person confirms. `playbook` and `context` select an investigation. See [Case agent](../advanced/case-agent.md) and [Agent actions](../advanced/agent-actions.md).

## UI

The web app exposes Agent Chat at `/app/agent`, including optional document upload for context. With `?caseId=` in the URL the page opens the case agent.

To keep a conversation under a stored session ID and resume it from any process, see [Agent sessions](./agent-sessions.md). Agent sessions are for apps that act as a signed-in user: they authenticate with the user's access token, not an API key.
