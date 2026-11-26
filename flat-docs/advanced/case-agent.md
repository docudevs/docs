---
title: Case agent
description: Chat with an agent bound to one case. It follows the case's instructions, works only on that case and proposes changes that a person confirms.
sidebar_position: 8
---
<!-- Generated from docs/advanced/case-agent.md. Do not edit by hand. -->

# Case agent

The case agent is the chat assistant opened on one case. It follows the case's **active instructions** from the [case memo](./case-memo.md), works only on that case through case-bound tools, and proposes changes instead of making them. Writes wait in the [pending-action inbox](./agent-actions.md) until a person confirms them.

The agent orchestrates and investigates. It can start a classify job from instructions, read the results, investigate flagged rows and propose corrections, instructions or questions. It does not classify rows itself: a [classify job](./classification.md) runs the rows in parallel, and the agent works on the job's results.

## Start a case chat

`POST /agent/chat` takes the messages of the general [Agent Chat](../core/agent-chat.md) and three optional fields:

| Field | Meaning |
|-------|---------|
| `caseId` | The case the agent works on. The case must belong to your organization; otherwise `404`. |
| `playbook` | `general` (the default) or `investigate-flagged`. Any other value is `400`. Playbooks need a `caseId`. |
| `context` | Ids only. `classify:{jobId}:{rowId}` points the agent at one row of a classify job. `run:{runId}` replies to a [headless run](./agent-runs.md): the agent reads the run's report and questions, and a run of another case is `404`. Any other format is `400`. |

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
      {"role": "user", "content": "Classify this week file into levels 4 and 5 using the memo rules."}
    ],
    "sessionId": "week-41",
    "caseId": 42,
    "playbook": "general"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

STATUS_JSON=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$STATUS_JSON" | jq -r '.response.message // empty'
echo "$STATUS_JSON" | jq -r '.pendingActions[]? | "\(.id) \(.summary)"'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    status = await client.agent_chat_and_wait(
        "Classify this week file into levels 4 and 5 using the memo rules.",
        session_id="week-41",
        case_id=42,
        playbook="general",
    )

    print((status.get("response") or {}).get("message"))
    for action in status.get("pendingActions") or []:
        print(action["id"], action["summary"])


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.AgentChatOptions;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
        .apiKey(System.getenv("API_KEY"))
        .baseUrl("https://api.docudevs.ai")
        .build();

JsonNode status = client.agentChatAndWait(
        "Classify this week file into levels 4 and 5 using the memo rules.",
        AgentChatOptions.builder()
                .sessionId("week-41")
                .caseId(42L)
                .playbook("general")
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .build());
System.out.println(status.path("response").path("message").asText(null));
for (JsonNode action : status.path("pendingActions")) {
    System.out.println(action.path("id").asText() + " " + action.path("summary").asText());
}
```

A session belongs to one organization and one case. A `sessionId` sent for another case starts a new history. Session ids are letters, digits, `-` and `_`, up to 64 characters; anything else is `400`.

The chat is a job, as in the general agent: the cURL variant polls `GET /agent/status/{jobGuid}` until it completes and the Python and Java helpers wait for you. The status carries `pendingActions`: a summary of each action the turn proposed (`id`, `tool`, `toolClass`, `summary`, `expiresAt`). Confirm or reject them with the [pending-action endpoints](./agent-actions.md); a chat started with an API key can be decided with any API key of the organization, see [who can confirm or reject](./agent-actions.md#who-can-confirm-or-reject).

## Which cases you can use

A call with an organization API key resolves the case without a user: it works on organization-wide cases, which are the cases created with an API key (see [Cases](./cases.md)), and on knowledge bases. A case created by a signed-in user answers `404` to an API key. The same holds for the memo, runs, evaluation and search calls on the other pages.

## What the agent sees

- **Instructions.** The active global instructions of the case memo, quoted as data, up to about 4,000 tokens. Proposed and retired entries are left out. The agent cannot change them.
- **Case-bound tools.** Every tool call is bound to the case in the chat. The model cannot name another case. Record, tree and classify tools are offered on records cases and knowledge bases; other cases get the tools that apply to them.
- **Tool results as data.** Record text, memo text and evidence come back wrapped as untrusted data, so text inside a record cannot give the agent instructions.
- **Session notes.** The agent keeps its plan and notes for the session. Durable knowledge goes to the memo through `add_feedback` and `propose_instruction`, which do not change classification until a person approves an instruction.

## Playbook: investigate flagged rows

Use `playbook: "investigate-flagged"` to work through the rows of a classify job that need review. The playbook works on at most 10 rows per turn:

1. Resume from the saved cursor if the session has one; otherwise start at the first flagged row.
2. List the flagged rows (`list_flagged_rows`) and group them by subtree and flag. The largest group comes first.
3. Read each row's evidence (`get_row_evidence`), the records it names (`get_record`) and the tree siblings (`search_categories`, `get_category`).
4. Read the memo and the pending actions first, so an existing proposal is cited and not repeated.
5. Make one proposal per row or group: a correction (`submit_review`), an instruction scoped to a subtree, or one specific question for a person.

The playbook never approves, accepts or confirms anything. Every proposal stays pending until a person acts on it. The turn saves its cursor in the session notes, so the next message continues where it stopped.

## Work from a row in the app

On a classify job's results, the **Ask agent about this row** link opens the agent at `/app/agent?caseId=…&context=classify:{jobId}:{rowId}`. The link carries ids only; the agent reads the row itself.

## Investigate flagged rows from the API

The chat request takes the same case fields (`caseId`, `playbook`, `context`) for every playbook. The Python helper `agent_chat_and_wait` and the Java `agentChatAndWait` also wait for the answer and return the completed status. This example points the investigate playbook at one row of a classify job:

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
      {"role": "user", "content": "Investigate the flagged rows of classify job 55."}
    ],
    "sessionId": "week-41",
    "caseId": 42,
    "playbook": "investigate-flagged",
    "context": "classify:55:17"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

STATUS_JSON=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$STATUS_JSON" | jq -r '.response.message // empty'
echo "$STATUS_JSON" | jq -r '.pendingActions[]? | "\(.id) \(.summary)"'
```

**Python**

```python
status = await client.agent_chat_and_wait(
    "Investigate the flagged rows of classify job 55.",
    session_id="week-41",
    case_id=42,
    playbook="investigate-flagged",
    context="classify:55:17",
)

print((status.get("response") or {}).get("message"))
for action in status.get("pendingActions") or []:
    print(action["id"], action["summary"])
```

**Java**

```java
import ai.docudevs.client.AgentChatOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

JsonNode status = client.agentChatAndWait(
        "Investigate the flagged rows of classify job 55.",
        AgentChatOptions.builder()
                .sessionId("week-41")
                .caseId(42L)
                .playbook("investigate-flagged")
                .context("classify:55:17")
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .build());
System.out.println(status.path("response").path("message").asText(null));
for (JsonNode action : status.path("pendingActions")) {
    System.out.println(action.path("id").asText() + " " + action.path("summary").asText());
}
```

## Reply to a headless run

A [headless run](./agent-runs.md) saves a report with questions for a person. Reply to it with a chat on the same case whose `context` is `run:{runId}`; the agent reads the run's report and questions. A run of another case answers `404`.

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
      {"role": "user", "content": "Record that fiction books are filed by genre when the description names one."}
    ],
    "sessionId": "run-9001-reply",
    "caseId": 42,
    "playbook": "general",
    "context": "run:9001"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

STATUS_JSON=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$STATUS_JSON" | jq -r '.response.message // empty'
echo "$STATUS_JSON" | jq -r '.pendingActions[]? | "\(.id) \(.summary)"'
```

**Python**

```python
status = await client.agent_chat_and_wait(
    "Record that fiction books are filed by genre when the description names one.",
    session_id="run-9001-reply",
    case_id=42,
    playbook="general",
    context="run:9001",  # run:{runId}, the id returned when the run was started
)

print((status.get("response") or {}).get("message"))
for action in status.get("pendingActions") or []:
    print(action["id"], action["summary"])
```

**Java**

```java
import ai.docudevs.client.AgentChatOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

JsonNode status = client.agentChatAndWait(
        "Record that fiction books are filed by genre when the description names one.",
        AgentChatOptions.builder()
                .sessionId("run-9001-reply")
                .caseId(42L)
                .playbook("general")
                .context("run:9001") // run:{runId}, the id returned when the run was started
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .build());
System.out.println(status.path("response").path("message").asText(null));
for (JsonNode action : status.path("pendingActions")) {
    System.out.println(action.path("id").asText() + " " + action.path("summary").asText());
}
```

To continue the conversation, send the next message with the same session id (`sessionId`, `session_id` in Python); a session keeps its history per case.

Any action the reply proposes waits in the [pending-action inbox](./agent-actions.md); the agent never confirms it.

## Related

- [Agent actions and the pending-action inbox](./agent-actions.md): confirming and rejecting what the agent proposes.
- [Headless agent runs](./agent-runs.md): the same agent run without a person in the chat.
- [Holdout evaluation and memo A/B](./case-evals.md): testing an instruction before it is approved.
