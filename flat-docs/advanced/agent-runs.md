---
title: Headless agent runs
description: Run the case agent without a person in the chat, from the API, a pipeline or a classify job. Bound each run with a token budget, read its report and get a webhook when it ends.
sidebar_position: 10
---
<!-- Generated from docs/advanced/agent-runs.md. Do not edit by hand. -->

# Headless agent runs

A headless run is the [case agent](./case-agent.md) working without a chat. You start it from the API, from a pipeline, or automatically when a [classify job](./classification.md) completes. The run works through its playbook in bounded turns, saves a report, and ends. It cannot confirm anything: its writes wait in the [pending-action inbox](./agent-actions.md) for a person.

A run acts as whoever started it: a signed-in user, or your organization when you start it with an API key. The agent in a run has the same limits as in a chat: it cannot approve, confirm, reject, start another run or cancel a run.

## Playbooks

| Playbook | Needs | Does |
|----------|-------|------|
| `investigate-flagged` | `params.jobId`: a classify job | Works through the flagged rows of the job, the same way as the [chat playbook](./case-agent.md#playbook-investigate-flagged-rows). |
| `general` | `goal`, up to 2,000 characters | Works towards the goal with the general agent. |

## Start a run

`POST /cases/{caseId}/agent/runs` returns `202` with `{runId, jobGuid}`.

```json
{
  "playbook": "investigate-flagged",
  "params": {"jobId": 55, "maxRows": 10, "flags": ["declined", "sourcesDisagree"]},
  "budget": {"maxTotalTokens": 60000}
}
```

| Field | Meaning |
|-------|---------|
| `params.jobId` | Classify job whose flagged rows are investigated. Required by `investigate-flagged`. |
| `params.maxRows` | Rows the run investigates in total, 1 to 500. Playbook default when absent. |
| `params.flags` | Flags that make a row worth investigating. Playbook default when absent. |
| `budget.maxTotalTokens` | Tokens the run may use in total, language model and authorised classify work together. At least 1. |

**cURL**

```bash
CASE_ID=42  # id returned when the case was created
CLASSIFY_JOB_ID=55  # id of a completed classify job of this case

wait_for_run() {
  local deadline=$((SECONDS + 1800))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED|CANCELLED) echo "Run $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Run $1 did not complete in time" >&2
  return 1
}

REQUEST=$(jq -n --argjson jobId "$CLASSIFY_JOB_ID" '{
  playbook: "investigate-flagged",
  params: {jobId: $jobId, maxRows: 10},
  budget: {maxTotalTokens: 60000}
}')

RUN_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/agent/runs" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$REQUEST" | jq -r '.runId')

wait_for_run "$RUN_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$RUN_ID" \
  -H "Authorization: $API_KEY" | jq '{summary: .report.summary, questions: (.report.questions // [])}'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id returned by create_case
    classify_job_id = 55  # id of a completed classify job of this case

    started = await client.start_agent_run(
        case_id,
        "investigate-flagged",
        params={"job_id": classify_job_id, "max_rows": 10},
        budget={"max_total_tokens": 60000},
    )

    # Raises AgentRunFailed when the run fails or is cancelled, and TimeoutError after 1800 seconds
    run = await client.wait_for_agent_run(
        case_id,
        started["runId"],
        timeout=1800,
        on_progress=lambda status: print(status["status"], status["turnsDone"]),
    )

    report = run.get("report") or {}
    print(report.get("summary"))
    for question in report.get("questions") or []:
        print("question:", question)


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.AgentRunOptions;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
        .apiKey(System.getenv("API_KEY"))
        .baseUrl("https://api.docudevs.ai")
        .build();

long caseId = 42L; // id returned by create_case
long classifyJobId = 55L; // id of a completed classify job of this case

JsonNode started = client.startAgentRun(
        caseId,
        "investigate-flagged",
        AgentRunOptions.builder()
                .params(Map.of("jobId", classifyJobId, "maxRows", 10))
                .budget(Map.of("maxTotalTokens", 60000))
                .build());

// Throws DocuDevsApiException (409) when the run fails or is cancelled, and
// DocuDevsTimeoutException after 1800 seconds
JsonNode run = client.waitForAgentRun(
        caseId,
        started.path("runId").asLong(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(1800))
                .pollInterval(Duration.ofSeconds(5))
                .build(),
        status -> System.out.println(status.path("status").asText() + " " + status.path("turnsDone").asText()));
JsonNode report = run.path("report");
System.out.println(report.path("summary").asText(null));
for (JsonNode question : report.path("questions")) {
    System.out.println("question: " + question);
}
```

**CLI**

```bash
cat > params.json <<'EOF'
{"job_id": 55, "max_rows": 10}
EOF

docudevs agent run start 42 investigate-flagged --params-file params.json --budget-tokens 60000 --wait
```

The cURL, Python and Java variants start the run, wait for it to end and print its report; the CLI starts it and, with `--wait`, waits too. The wait ends when the run is `COMPLETED`, and fails when it is `FAILED` or `CANCELLED`. The Python SDK takes `max_rows` and `job_id` in `params` and `max_total_tokens` in `budget`; the request itself uses `maxRows`, `jobId` and `maxTotalTokens`, which the Java variant passes as written. The CLI reads `max_rows`, `flags` and `job_id` from the params file.

A run continues in further turns until it has investigated `maxRows`, uses its budget, is cancelled, or reaches 100 turns. The 45-step limit applies to each turn, not to the run.

`400` means an unknown playbook, a missing goal or job, or invalid params or budget. `404` means the case does not exist. `503` means the run could not be queued; retry.

## Budget

`maxTotalTokens` is checked **before each model call**, not only between turns. A turn refuses a further model call once the remaining budget is spent, so a run overshoots by **at most one model call**. Costly tools run inside the budget without a confirmation, and they stop when the budget is spent.

When the budget is used up, the run **completes** (status `COMPLETED`, not `FAILED`). The reason is in `error`, for example `Token budget exhausted: 61200 of 60000 tokens used over 3 turns; 4 rows investigated.` The API writes a report of the progress without another model call, so the work done is never lost. `remainingTokens` in the status shows what is left.

Without a budget, the run stops at `maxRows`, at the turn limit, or when it has nothing more to do.

## Read a run

`GET /cases/{caseId}/agent/runs/{runId}` returns the state:

| Field | Meaning |
|-------|---------|
| `status` | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` or `CANCELLED`. |
| `trigger` | `api`, `classify` or `pipeline`. |
| `originJobId` | The classify job the run was started from, if any. |
| `turnsDone`, `rowsDone`, `tokensUsed`, `remainingTokens` | Progress and budget. `remainingTokens` is absent without a budget. |
| `cursor` | Where the next turn continues. |
| `pendingActionIds` | Every [pending action](./agent-actions.md) the run proposed, in any status. |
| `report` | `summary`, `proposals` (by reference) and `questions`. Absent until the run saves one. |
| `error` | Why the run failed, or the budget reason when it stopped on the budget. |

`originJobId` (optional) names the classify job the run investigates. It must be a classify job of the same case; otherwise the request returns `404`. The run is then listed under that job.

`GET /cases/{caseId}/agent/runs` lists the runs of a case, newest first, with `limit` (1 to 200, default 50), `continuation`, `status` and `originJobId`. Filter by `originJobId` to see the runs of one classify job.

**cURL**

```bash
CASE_ID=42  # id returned when the case was created
CLASSIFY_JOB_ID=55  # id of a classify job of this case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs?originJobId=$CLASSIFY_JOB_ID&status=COMPLETED" \
  -H "Authorization: $API_KEY" | jq -r '(.items // [])[] | "\(.runId) \(.status) \(.tokensUsed)"'
```

**Python**

```python
case_id = 42  # id returned by create_case
classify_job_id = 55  # id of a classify job of this case

runs = await client.list_agent_runs(case_id, origin_job_id=classify_job_id, status="COMPLETED")
for run in runs.get("items", []):
    print(run["runId"], run["status"], run["tokensUsed"])
```

**Java**

```java
import ai.docudevs.client.AgentRunListOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id returned by create_case
long classifyJobId = 55L; // id of a classify job of this case

JsonNode runs = client.listAgentRuns(
        caseId,
        AgentRunListOptions.builder().originJobId(classifyJobId).status("COMPLETED").build());
for (JsonNode run : runs.path("items")) {
    System.out.println(run.path("runId").asText() + " " + run.path("status").asText()
            + " " + run.path("tokensUsed").asText());
}
```

## Cancel a run

`POST /cases/{caseId}/agent/runs/{runId}/cancel` marks the run `CANCELLED` immediately; a turn already in progress may still finish. Cancelling a cancelled run returns the same state. A run that has already finished returns `409`.

**cURL**

```bash
CASE_ID=42  # id returned when the case was created
RUN_ID=9001  # id returned when the run was started

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$RUN_ID/cancel" \
  -H "Authorization: $API_KEY" | jq '{runId, status}'
```

**Python**

```python
case_id = 42  # id returned by create_case
run_id = 9001  # id returned when the run was started

cancelled = await client.cancel_agent_run(case_id, run_id)
print(cancelled["runId"], cancelled["status"])
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id returned by create_case
long runId = 9001L; // id returned when the run was started

JsonNode cancelled = client.cancelAgentRun(caseId, runId);
System.out.println(cancelled.path("runId").asText() + " " + cancelled.path("status").asText());
```

**CLI**

```bash
docudevs agent run cancel 42 9001
```

## Classify trigger

A classify job can start a run when it completes. Add `investigate` to the job config:

```json
{
  "config": {
    "investigate": {
      "enabled": true,
      "playbook": "investigate-flagged",
      "maxRows": 10,
      "maxTotalTokens": 60000,
      "flags": ["declined", "modelNamedDifferentBestFit", "sourcesDisagree"]
    }
  }
}
```

`playbook` defaults to `investigate-flagged`, `maxRows` to 10 (1 to 500), and `flags` to `declined`, `modelNamedDifferentBestFit` and `sourcesDisagree`. The run's `originJobId` is the job, so the job's results and the run's proposals can be read together.

`maxTotalTokens` (optional, at least 1) bounds the run's tokens, as `budget.maxTotalTokens` does for a direct run. When it is absent the run takes a platform default budget, so a triggered run is never unbounded. Its costly tools become pending actions for a person.

The run starts when the classify job completes. A job that fails or is cancelled starts no run.

**cURL**

```bash
CASE_ID=42  # id of a records case with an active category tree

wait_for_classify() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/classify/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED|CANCELLED) echo "Classify job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Classify job $1 did not complete in time" >&2
  return 1
}

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "keys": ["SKU-1001", "SKU-1002", "SKU-1003"],
    "config": {
      "maxTotalTokens": 400000,
      "investigate": {"enabled": true, "maxRows": 10}
    }
  }' | jq -r '.jobId')

wait_for_classify "$JOB_ID"
echo "Classify job $JOB_ID completed; the investigation run starts from it"
```

**Python**

```python
case_id = 42  # id of a records case with an active category tree
keys = ["SKU-1001", "SKU-1002", "SKU-1003"]  # record keys of the case's records

# Raises ClassifyJobFailed when the job fails or is cancelled, and TimeoutError after 3600 seconds
job = await client.classify(
    case_id,
    keys=keys,
    config={"maxTotalTokens": 400000, "investigate": {"enabled": True, "maxRows": 10}},
    wait=True,
)
print(f"Classify job {job['jobId']} completed; the investigation run starts from it")
```

**Java**

```java
import ai.docudevs.client.ClassifyOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case with an active category tree
List<String> keys = List.of("SKU-1001", "SKU-1002", "SKU-1003"); // record keys of the case's records

// Throws DocuDevsApiException (409) when the job fails or is cancelled, and
// DocuDevsTimeoutException after 3600 seconds
JsonNode job = client.classifyKeysAndWait(
        caseId,
        keys,
        ClassifyOptions.builder()
                .maxTotalTokens(400000L)
                .investigate(Map.of("enabled", true, "maxRows", 10))
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(3600))
                .pollInterval(Duration.ofSeconds(2))
                .build());
System.out.println("Classify job " + job.path("jobId").asText() + " completed; the investigation run starts from it");
```

The example sends the `investigate` settings in the job's `config` and waits for the job. The job's own `maxTotalTokens` bounds the classify work only; the run takes its own budget from `investigate.maxTotalTokens`. Read the run afterwards by listing the runs of the job, as above.

## Webhook

When the run's job ends, the organization's [webhook](../integration/webhooks.md) fires once, with the run's `jobGuid`, `status` and `error`. Use it as a signal and then read the run for its report.
