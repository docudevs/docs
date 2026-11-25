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

<Example name="agent-chat/chat-with-agent" />

The request starts a chat job and returns its `jobGuid` and the `sessionId`. Poll `GET /agent/status/{jobGuid}` until `status` is `COMPLETED`; the answer is in `response.message`. A job that ends in `ERROR` carries the reason in `error`. The cURL variant and the Python helper both give up after 120 seconds.

## Case-bound chat

Pass `caseId` to work on one case. The agent then follows the case's active memo instructions, works through case-bound tools and proposes changes that a person confirms. `playbook` and `context` select an investigation. See [Case agent](../advanced/case-agent.md) and [Agent actions](../advanced/agent-actions.md).

## UI

The web app exposes Agent Chat at `/app/agent`, including optional document upload for context. With `?caseId=` in the URL the page opens the case agent.

To keep a conversation under a stored session ID and resume it from any process, see [Agent sessions](./agent-sessions.md). Agent sessions are for apps that act as a signed-in user: they authenticate with the user's access token, not an API key.
