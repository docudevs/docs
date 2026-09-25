---
title: Client Pipelines
description: Mix remote DocuDevs extraction nodes with local Python functions in one hybrid, SDK-orchestrated pipeline run.
---

`ClientPipeline` is a Python SDK construct that mixes remote DocuDevs nodes (`extract`, `ocr_correct`, `operation`) with live Python callables (`local_function`) that run in your own process — for example deterministic post-processing, a database lookup, or a business-rule check that should not cost an LLM call. The SDK compiles the mixed graph into one or more ordinary [pipeline mode](/docs/reference/pipeline-mode) segments, submits each segment to the API in turn, runs your local functions locally in between, and assembles the final result.

Client pipelines are Python SDK only today — there is no CLI command and no direct REST usage pattern for a caller. The value is entirely in-process orchestration across several authenticated calls, so a curl equivalent would not be a realistic usage path.

## How it differs from server pipeline mode

A plain [pipeline](/docs/reference/pipeline-mode) runs entirely on the server: you build a `Pipeline`, submit it once, and the server executes the whole graph. A `ClientPipeline` still compiles to that same pipeline JSON wire contract, but the SDK splits the graph at every `local_function` node into separate segments, submits each segment as its own pipeline-mode job step, and runs your callable between segments. Need a step to route, repair, or assemble entirely on the server? Use `Pipeline`. Need a step to run local business logic — a database call, a deterministic computation, code that cannot run inside the DocuDevs worker — between remote steps? Use `ClientPipeline`.

Because a Python callable cannot be serialized, a `ClientPipeline` can never be saved as a named server configuration, and a run is **not resumable**: if your process stops mid-run, a later attempt executes the graph from the beginning.

## Building the graph

```python
from docudevs import ClientPipeline, DocuDevsClient, P
```

`ClientPipeline()` takes an optional `version` (default `"2026-05"`, the same pipeline wire-format version used by `Pipeline`). It exposes:

- `extract(node_id, *, prompt=None, schema=None, source=None, input=None, tools=None, llm_tier=None, depends_on=None, when=None, on_error=None)` — a remote extraction node.
- `ocr_correct(node_id, *, source=None, depends_on=None, when=None, mode="correction", scope="pagewise", output_format="markdown")` — a remote OCR-correction node.
- `operation(node_id, *, operation, input=None, depends_on=None, when=None)` — a remote operation node.
- `local_function(node_id, fn, *, input=None, output_type=None, depends_on=None, when=None, on_error=None)` — a local node backed by a Python callable.
- `final(ref)` — marks the graph's published result.
- `validate()` — checks the graph for cycles, unknown references, and branching rules before you run it (also called automatically when you execute the pipeline).

These are the only three remote node types a `ClientPipeline` can build; there is no client-side `transform`, `validate`, or `final`-as-a-node builder like the server `Pipeline` has.

Every builder method returns a `NodeRef`. Reference a prior node's result the same way you would in a server pipeline — with `.result` (optionally followed by a path), which becomes a `$nodes.<id>.result...` reference on the wire:

```python
cp = ClientPipeline()
summary = cp.extract(
    "summary",
    prompt="List the distinct topics this document covers.",
    schema={
        "type": "object",
        "properties": {"topics": {"type": "array", "items": {"type": "string"}}},
        "required": ["topics"],
    },
    source=P.ocr.content,
)
```

### Local functions

`local_function` accepts a sync or async callable. A sync function runs off the event loop via `asyncio.to_thread`, so a slow computation does not stall segment polling. Its `input` mapping is resolved against completed node results and passed to the callable as **keyword arguments** — so `input={"summary": summary.result}` calls `fn(summary=...)`.

```python
from dataclasses import dataclass

@dataclass
class TopicCount:
    count: int

def count_topics(*, summary) -> TopicCount:
    topics = (summary or {}).get("topics") or []
    return TopicCount(count=len(topics))

counted = cp.local_function(
    "counted",
    count_topics,
    input={"summary": summary.result},
    output_type=TopicCount,
)
```

The callable's return value is normalized to plain JSON before it can be referenced by later nodes: a pydantic v2 model is dumped with `model_dump(mode="json")`, a dataclass with `dataclasses.asdict`, anything else passed through as-is. The normalized value must be JSON-serializable, or the run fails with a clear error. When `output_type` is a pydantic model or a dataclass, the normalized value is validated against it before the node is marked complete.

`local_function` also accepts `on_error`, mirroring the remote node builders, but the current runner does not branch on it: any exception raised by a local callable (or a failed output validation) aborts the entire client-run — the run is marked failed and the DocuDevs job's client run is explicitly failed server-side. Do not rely on `on_error="continue"` for local nodes.

A downstream node — remote or local — can depend on a value produced by either kind of node uniformly; `$nodes.<id>.result...` references work the same way regardless of whether `<id>` is a remote or a local node.

### Conditional branching

Any node accepts a `when` condition, built with the same helpers used by server pipelines (`eq`, `ne`, `exists`, `and_`, `or_`, `not_`, importable from `docudevs`). A graph with any conditioned node must have exactly one `final`, and that final node must be an unconditional local node — this is enforced by `validate()`. A conditioned **remote** node also cannot set `on_error="continue"`.

```python
route = cp.local_function("route", lambda: {"positive": True})
guarded = cp.extract(
    "guarded",
    prompt="Return one object with boolean field observed.",
    schema={"type": "object", "properties": {"observed": {"type": "boolean"}}, "required": ["observed"]},
    when={"eq": [route.result.positive, True]},
)
assembled = cp.local_function(
    "assembled",
    lambda *, route, guarded: {"positive": route["positive"], "guarded": guarded},
    input={"route": route.result, "guarded": guarded.result},
)
cp.final(assembled)
```

When a conditioned segment's gate evaluates false, its remote nodes are skipped (their result becomes `None`) rather than executed, and any local node consuming that result must also tolerate `None`. A consumer's condition must imply the condition of every conditioned node it reads from — the compiler rejects a graph where that is not the case.

## Running the pipeline

```python
client = DocuDevsClient(api_url="https://api.docudevs.ai", token=os.getenv("API_KEY"))

verdict = cp.extract(
    "verdict",
    prompt=(
        "You are given a topic count computed from this document. "
        "Answer whether it matches the number of distinct topics you see."
    ),
    schema={
        "type": "object",
        "properties": {"topic_count_matches": {"type": "boolean"}},
        "required": ["topic_count_matches"],
    },
    input={"counts": counted.result},
)
cp.final(verdict)

with open("report.pdf", "rb") as handle:
    run = await client.run_client_pipeline(
        handle.read(),
        "application/pdf",
        pipeline=cp,
        timeout=900.0,
        poll_interval=2.0,
        on_event=lambda name, payload: print(name, payload),
    )

print(run.final_result)
```

`run_client_pipeline(document, document_mime_type, *, pipeline, timeout=900.0, poll_interval=2.0, on_event=None)` uploads the document, opens a client run, and executes every segment of the compiled graph in order — refreshing the run's server-side TTL on a heartbeat while a segment or local function is in flight, running each local node as soon as its dependencies are satisfied, uploading only the local results a later segment actually consumes, and polling each submitted segment's status until it completes. It returns a `ClientPipelineRun`:

```python
@dataclass
class ClientPipelineRun:
    guid: str
    client_run_id: str | None
    final_result: Any
    chosen_final: str | None
    nodes: dict[str, NodeReport]       # id, kind ("remote"/"local"), status, started_at/completed_at, error
    segments: list[SegmentReport]      # index, status, started_at/completed_at, is_final
    published: bool
    version: str
```

`on_event` is called synchronously with an event name and a payload dict as the run progresses. Event names emitted by the runner: `run_started`, `segment_started`, `segment_completed`, `segment_skipped`, `local_node_started`, `local_node_completed`, `node_skipped`, `node_failed`, `run_completed`.

## How compilation works (advanced)

You do not need this section to use `ClientPipeline` — it explains what happens under the hood.

The SDK groups every remote node reachable without crossing a local-function boundary into one server pipeline segment (a remote-on-remote dependency does not by itself force a new segment; only a dependency on a local result, or a different `when` gate, does). Values produced by a prior segment are threaded into the next one through the same `inputs` / `$inputs` mechanism used by server pipeline mode: a value produced remotely is read back as a `nodeArtifact`, and a value produced by a local function is uploaded as a `clientArtifact`.

This is backed by five endpoints not otherwise part of the public SDK surface (`docudevs.yaml`, used by `ClientRunApi`):

- `POST /job/{guid}/pipeline/client-runs` — open a run, returns a `clientRunId`.
- `PUT /job/{guid}/pipeline/client-runs/{clientRunId}/nodes/{nodeId}/result` — upload a local node's JSON result as a client artifact.
- `GET /job/{guid}/pipeline/client-runs/{clientRunId}/segments/{segmentIndex}/status` — poll a submitted segment.
- `POST /job/{guid}/pipeline/client-runs/{clientRunId}/refresh` — extend the run's TTL (used by the heartbeat).
- `POST /job/{guid}/pipeline/client-runs/{clientRunId}/fail` — mark the run failed.

Each segment is itself submitted as an ordinary `PIPELINE`-mode job step against `POST /document/process/{guid}`, with `clientRunId`, `segmentIndex`, and `final` query parameters identifying it as part of a client run.
