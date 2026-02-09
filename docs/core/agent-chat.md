---
title: Agent Chat
description: Conversational AI agent for iterative document analysis with multi-turn chat sessions and document context.
sidebar_position: 4
---

# Agent Chat

Agent Chat provides an interactive assistant that helps you design extraction configurations and answer document-related questions. It runs on a dedicated queue so chat requests stay responsive while document processing workloads are running.

## How It Works

1. Send a chat message with optional `sessionId` and `documentGuid`.
2. The API queues the request and returns a job GUID.
3. Poll the status endpoint until the response is complete.

## API Usage

### Start a chat

```bash
curl -X POST https://api.docudevs.ai/agent/chat \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "Help me build an invoice extraction schema"}
    ],
    "sessionId": "session-123"
  }'
```

### Poll for status

```bash
curl -X GET https://api.docudevs.ai/agent/status/<JOB_GUID> \
  -H "Authorization: $API_KEY"
```

## Python SDK

```python
import asyncio
from docudevs.docudevs_client import DocuDevsClient

async def chat_with_agent():
    client = DocuDevsClient(token="your-api-key")

    response = await client.agent_chat_and_wait(
        messages=[{"role": "user", "content": "Extract line items from invoices"}],
        session_id="session-123",
    )

    print(response["response"]["message"])

asyncio.run(chat_with_agent())
```

## UI

The web app exposes Agent Chat at `/app/agent`, including optional document upload for context.

## Self-Hosted Configuration

Set the agent queue name for both API and worker services:

- API: `AZURE_AGENT_QUEUE_NAME`
- Worker: `AGENT_QUEUE_NAME` or `AZURE_AGENT_QUEUE_NAME`
