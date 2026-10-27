---
title: Case agent
description: Chat with an agent bound to one case. It follows the case's instructions, works only on that case and proposes changes that a person confirms.
sidebar_position: 8
---

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

```bash
curl -X POST https://api.docudevs.ai/agent/chat \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "Classify this week file into levels 4 and 5 using the memo rules."}
    ],
    "sessionId": "week-41",
    "caseId": 42,
    "playbook": "general"
  }'
```

A session belongs to one organization and one case. A `sessionId` sent for another case starts a new history. Session ids are letters, digits, `-` and `_`, up to 64 characters; anything else is `400`.

The chat is a job, as in the general agent. Poll `GET /agent/status/{jobGuid}` until it completes. The status carries `pendingActions`: a summary of each action the turn proposed (`id`, `tool`, `toolClass`, `summary`, `expiresAt`). Confirm or reject them with the [pending-action endpoints](./agent-actions.md).

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

## Python SDK

`agent_chat_and_wait` takes the same case arguments as `agent_chat` and returns the completed status:

```python
status = await client.agent_chat_and_wait(
    [{"role": "user", "content": "Investigate the flagged rows of classify job 55."}],
    session_id="week-41",
    case_id=42,
    playbook="investigate-flagged",
    context="classify:55:17",
)

print(status["response"]["message"])
for action in status.get("pendingActions") or []:
    print(action["id"], action["summary"])
```

### Reply to the agent

A conversation keeps the session for you, so each reply sends only your new message. `run.conversation()` starts one about a headless run, so you can answer the questions in its report:

```python
run = await case.agent_run_and_wait("investigate-flagged", job=job, max_tokens=250_000)
print((run.report or {}).get("questions"))

chat = run.conversation()
reply = await chat.reply("Record that packaged roasted coffee (beans or ground) is 8.2.2 Coffees.")
print(reply.message)
for action in reply.pending_actions:  # the actions this reply proposed
    print(action.tool, "-", action.summary)
    await action.confirm()  # you confirm; the agent never does

reply = await chat.reply("Also correct row 28 of that job.")
```

`case.conversation(context="classify:55:17")` starts a conversation about one row, and `case.conversation(session_id=chat.session_id)` resumes an earlier one. `reply()` waits up to 600 seconds by default.

## Status of the flows

The agent flows below were run on the MINI tier in the P3 live check (2026-10-09). The check used a real case and kept its data out of the repository.

Confirmed live:

- The agent reads the case instructions and quotes the active one.
- The agent proposes a classify job; after a person confirms it, the job runs to completion.
- The agent proposes a review; after confirmation the record carries the new label with `labelSource=review`.
- The agent's attempt to approve a memo entry stays pending, and a direct approve with an agent token returns `403`.
- An investigate turn, after the step-limit fixes, reads row evidence in about 5 to 6 tool calls and ends with a question.
- A headless run started after a classify job completes, is listed under that job, and saves its report.
- A memo A/B evaluation runs through the evaluation endpoint.
- A headless run that nears its budget saves its report before it stops (demo, 2026-10-27: 68,078 of 80,000 tokens, summary and one question saved).

Not yet confirmed live:

- A classify job whose input rows are read from the records table (the keys source) after its fix. Inline rows on the same worker succeeded.
- An agent-proposed holdout evaluation started from the chat. The A/B run was started directly.
- The `RECORDS_MATCH` extraction tool.
- The webhook when an agent run ends.
- The case inbox and agent page in the web app. The live check ran against the API.
- A classify job with no target depth on a case without `classifyDefaults`; the tree-level default is tested but was not run live after the fix.

## Related

- [Agent actions and the pending-action inbox](./agent-actions.md): confirming and rejecting what the agent proposes.
- [Headless agent runs](./agent-runs.md): the same agent run without a person in the chat.
- [Holdout evaluation and memo A/B](./case-evals.md): testing an instruction before it is approved.
