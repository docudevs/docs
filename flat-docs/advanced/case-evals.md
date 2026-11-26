---
title: Holdout evaluation and memo A/B
description: Measure a classification setup against held-out labelled records, compare two runs with paired differences, and test a proposed memo instruction before it is approved.
sidebar_position: 11
---
<!-- Generated from docs/advanced/case-evals.md. Do not edit by hand. -->

# Holdout evaluation and memo A/B

A holdout evaluation tells you how well a classification setup would have done on records whose labels you already know. It takes a **grouped sample** of labelled records, classifies each sampled record once per variant with the held-out records left out of the evidence, and scores the results against the historical labels.

Use it to answer questions such as: does a new memo instruction help, does a different tree version or candidate strategy do better, does a different model tier change accuracy. The evaluation is **evidence for the reviewer**. It does not approve anything, and approval stays a human decision in the [case memo](./case-memo.md).

## How a holdout run works

1. **Sample.** The evaluation draws `sampleSize` labelled records (10 to 5,000, default 200). By default the grouping is `nearDuplicate`: records that are near-duplicates of a sampled record are also kept out of its evidence. A `groupField` groups by a metadata key instead, and `grouping.mode` `none` opts out (see [Grouping](#grouping)). The sample seed is stored, so runs with the same seed are comparable.
2. **Variants.** Each variant is one classify job over the same sample. Variants can change the memo (`memoInclude` adds proposed entries on top of the active memo), or the classify config. The first variant is the baseline.
3. **Leave-out.** A held-out record is removed from the candidate evidence for its own run. The evaluation over-fetches candidates and drops held-out keys; the report states the effective candidate count and any shortfall.
4. **Score.** Results are scored against the labels: accuracy per tree level, accuracy at the target depth, the candidate ceiling (the correct code was among the candidates), decline rate, and coverage against accuracy over the routing flags.

Historical labels can contain errors, and conventions can change. A holdout score measures agreement with past filing, not correctness against a new policy, and the report says so.

## Start an evaluation

`POST /cases/{caseId}/evals/holdout` returns `202` with `{evalId, jobGuid, status}`.

```json
{
  "sampleSize": 200,
  "seed": 7,
  "grouping": {"mode": "nearDuplicate", "threshold": 0.98},
  "config": {"llmLevel": "MINI", "targetDepth": [4, 5]},
  "variants": [
    {"name": "baseline"},
    {"name": "with-rule", "memoInclude": ["m_88"]}
  ]
}
```

| Field | Meaning |
|-------|---------|
| `variants` | One to four. `name` is unique within the evaluation, up to 64 characters. |
| `variants[].memoInclude` | Memo entry ids, proposed ones included, loaded on top of the active memo for that variant. |
| `variants[].config` | Classify settings for this variant. It replaces the shared `config` when present. |
| `config` | Classify settings shared by the variants. |
| `grouping` | How near-duplicates of the sample are kept out of its evidence. See [Grouping](#grouping). Omitted means `nearDuplicate` at the default threshold. |
| `groupField` | A record metadata key. Records that share its value are held out together. Cannot be combined with `grouping`, unless `grouping.mode` is `field`. |

The evaluation pins the effective target depth in its spec, so every variant reports the same levels.

`GET /cases/{caseId}/evals/{evalId}` returns the status, each variant's classify job and status, the sample hash, the records table version, the tree version and the memo version, and, once it completes, the `report`. The report has `sample` (size, requested size, seed, group field, grouping, sample hash, excluded records), `levels`, `variants` (score, failed rows, usage, leave-out), `comparisons` with paired differences per level, and `warnings`. A run with grouping `none` carries the warning `ungrouped`.

An evaluation is asynchronous. Its `status` moves through `PREPARING`, `RUNNING` and `SCORING` to `COMPLETED` or `FAILED`; the `report` is present once it is `COMPLETED`, and `error` says why a run `FAILED`.

**cURL**

```bash
CASE_ID=42  # id of a records case with an active category tree
ENTRY_ID=m_88  # id of a proposed memo entry

wait_for_eval() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED) echo "Evaluation $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Evaluation $1 did not complete in time" >&2
  return 1
}

REQUEST=$(jq -n --arg entry "$ENTRY_ID" '{
  sampleSize: 200,
  seed: 7,
  grouping: {mode: "nearDuplicate", threshold: 0.98},
  config: {llmLevel: "MINI"},
  variants: [
    {name: "baseline"},
    {name: "with-rule", memoInclude: [$entry]}
  ]
}')

EVAL_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/evals/holdout" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$REQUEST" | jq -r '.evalId')

wait_for_eval "$EVAL_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$EVAL_ID" \
  -H "Authorization: $API_KEY" | jq '(.report.variants // [])[] | {name, targetAccuracy: .score.targetAccuracy.all}'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case with an active category tree
    entry_id = "m_88"  # id of a proposed memo entry

    variants = [
        {"name": "baseline"},
        {"name": "with-rule", "memo_include": [entry_id]},
    ]

    started = await client.start_holdout_eval(
        case_id,
        variants,
        sample_size=200,
        seed=7,
        grouping={"mode": "nearDuplicate", "threshold": 0.98},
        config={"llmLevel": "MINI"},
    )

    # Raises EvalFailed when the evaluation fails, and TimeoutError after 3600 seconds
    finished = await client.wait_for_eval(case_id, started["evalId"], timeout=3600)

    for variant in finished["report"].get("variants", []):
        print(variant["name"], variant["score"]["targetAccuracy"].get("all"))


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.HoldoutEvalOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
        .apiKey(System.getenv("API_KEY"))
        .baseUrl("https://api.docudevs.ai")
        .build();

long caseId = 42L; // id of a records case with an active category tree
String entryId = "m_88"; // id of a proposed memo entry

List<Map<String, Object>> variants = List.of(
        Map.of("name", "baseline"),
        Map.of("name", "with-rule", "memoInclude", List.of(entryId)));

JsonNode started = client.startHoldoutEval(
        caseId,
        variants,
        HoldoutEvalOptions.builder()
                .sampleSize(200)
                .seed(7L)
                .grouping(Map.of("mode", "nearDuplicate", "threshold", 0.98))
                .config(Map.of("llmLevel", "MINI"))
                .build());

// Throws DocuDevsApiException (409) when the evaluation fails, and
// DocuDevsTimeoutException after 3600 seconds
JsonNode finished = client.waitForEval(
        caseId,
        started.path("evalId").asLong(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(3600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
for (JsonNode variant : finished.path("report").path("variants")) {
    System.out.println(variant.path("name").asText() + " "
            + variant.path("score").path("targetAccuracy").path("all").asText(null));
}
```

**CLI**

```bash
cat > variants.json <<'EOF'
[{"name": "baseline"}, {"name": "with-rule", "memo_include": ["m_88"]}]
EOF
cat > config.json <<'EOF'
{"llmLevel": "MINI"}
EOF

docudevs eval holdout 42 --variants-file variants.json --sample-size 200 --seed 7 --grouping-mode nearDuplicate --grouping-threshold 0.98 --config-file config.json --wait
```

The example starts the evaluation, waits for it and prints each variant's accuracy at the target depth. The Java variant writes `memoInclude` as in the request and sets the sample and grouping through `HoldoutEvalOptions`. The Python SDK takes `memo_include` in a variant and `sample_size` and `group_field` as keyword arguments, where the request uses `memoInclude`, `sampleSize` and `groupField`; the CLI reads `variants.json` as a list of `{"name", "memo_include"?, "config"?}` and its first entry is the baseline. To group by a record metadata key, pass `groupField` (`group_field` in Python, `--group-field` in the CLI) instead of the grouping options. `get_holdout_eval` in Python and `docudevs eval status` read the status without waiting.

## Compare two runs

`POST /cases/{caseId}/evals:compare` scores two completed evaluations against each other without classifying again. It answers `202` with `{evalId, jobGuid, status}`: the comparison is a new evaluation with status `SCORING`, and you read its `report.comparisons` from `GET /cases/{caseId}/evals/{evalId}` once it is `COMPLETED`.

```json
{"baselineEvalId": 301, "candidateEvalId": 302, "baselineVariant": "baseline", "candidateVariant": "with-rule"}
```

**cURL**

```bash
CASE_ID=42  # id of a records case with an active category tree
BASELINE_EVAL_ID=301  # ids of two completed holdout evaluations
CANDIDATE_EVAL_ID=302  # that drew the same sample

wait_for_eval() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED) echo "Evaluation $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Evaluation $1 did not complete in time" >&2
  return 1
}

REQUEST=$(jq -n --argjson baseline "$BASELINE_EVAL_ID" --argjson candidate "$CANDIDATE_EVAL_ID" '{
  baselineEvalId: $baseline,
  candidateEvalId: $candidate,
  baselineVariant: "baseline",
  candidateVariant: "with-rule"
}')

# Scoring is asynchronous: the answer carries the id of a new evaluation
COMPARE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/evals:compare" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$REQUEST" | jq -r '.evalId')

wait_for_eval "$COMPARE_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$COMPARE_ID" \
  -H "Authorization: $API_KEY" | jq '(.report.comparisons // [])[] | {baseline, candidate, target}'
```

**Python**

```python
case_id = 42  # id of a records case with an active category tree
baseline_eval_id = 301  # ids of two completed holdout evaluations
candidate_eval_id = 302  # that drew the same sample

# Scoring is asynchronous: the answer carries the id of a new evaluation
comparison = await client.compare_holdout_evals(
    case_id,
    baseline_eval_id=baseline_eval_id,
    candidate_eval_id=candidate_eval_id,
    baseline_variant="baseline",
    candidate_variant="with-rule",
)

# Raises EvalFailed when the scoring fails, and TimeoutError after 3600 seconds
scored = await client.wait_for_eval(case_id, comparison["evalId"], timeout=3600)

for pair in scored["report"].get("comparisons", []):
    print(pair["baseline"], pair["candidate"], pair["target"])
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case with an active category tree
long baselineEvalId = 301L; // ids of two completed holdout evaluations
long candidateEvalId = 302L; // that drew the same sample

// Scoring is asynchronous: the answer carries the id of a new evaluation
JsonNode comparison = client.compareHoldoutEvals(
        caseId, baselineEvalId, candidateEvalId, "baseline", "with-rule");

// Throws DocuDevsApiException (409) when the scoring fails, and
// DocuDevsTimeoutException after 3600 seconds
JsonNode scored = client.waitForEval(
        caseId,
        comparison.path("evalId").asLong(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(3600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
for (JsonNode pair : scored.path("report").path("comparisons")) {
    System.out.println(pair.path("baseline") + " " + pair.path("candidate") + " " + pair.path("target"));
}
```

**CLI**

```bash
docudevs eval compare 42 301 302 --baseline-variant baseline --candidate-variant with-rule

# Scoring is asynchronous: read the result with the evalId the compare command printed
docudevs eval status 42 303 --wait
```

The samples and the grouping must match: if the two evaluations drew different samples, or used a different grouping mode, field or threshold, the answer is `409`. Each comparison reports, for the target level and for every level in `byLevel`, the rows right in both runs (`bothRight`), right only in the baseline (`aOnly`), right only in the candidate (`bOnly`) and wrong in both (`bothWrong`), with a McNemar exact p-value (`pValue`). It does not check that the two runs used the same target depth, so compare runs that share their `targetDepth`.

## Test a proposed memo entry

`POST /cases/{caseId}/memo/entries/{entryId}/evaluate` is the shortcut for a memo A/B. Run A uses the active memo; run B adds the proposed entry. It takes the same optional `sampleSize`, `seed`, `groupField`, `grouping` and `config`, and it returns `202` with `{evalId, jobGuid, status}`, like a holdout evaluation. It answers `409` when the entry is not a proposed instruction.

**cURL**

```bash
CASE_ID=42  # id of a records case with an active category tree
ENTRY_ID=m_88  # id of a proposed memo entry

wait_for_eval() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED) echo "Evaluation $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Evaluation $1 did not complete in time" >&2
  return 1
}

EVAL_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/evaluate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"sampleSize": 200, "seed": 7, "groupField": "base_product_id"}' | jq -r '.evalId')

wait_for_eval "$EVAL_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$EVAL_ID" \
  -H "Authorization: $API_KEY" | jq '(.report.comparisons // [])[] | {baseline, candidate, target}'
```

**Python**

```python
case_id = 42  # id of a records case with an active category tree
entry_id = "m_88"  # id of a proposed memo entry

started = await client.evaluate_memo_entry(
    case_id, entry_id, sample_size=200, seed=7, group_field="base_product_id"
)

# Raises EvalFailed when the evaluation fails, and TimeoutError after 3600 seconds
finished = await client.wait_for_eval(case_id, started["evalId"], timeout=3600)

for pair in finished["report"].get("comparisons", []):
    print(pair["baseline"], pair["candidate"], pair["target"])
```

**Java**

```java
import ai.docudevs.client.MemoEvaluationOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case with an active category tree
String entryId = "m_88"; // id of a proposed memo entry

JsonNode started = client.evaluateMemoEntry(
        caseId,
        entryId,
        MemoEvaluationOptions.builder()
                .sampleSize(200)
                .seed(7L)
                .groupField("base_product_id")
                .build());

// Throws DocuDevsApiException (409) when the evaluation fails, and
// DocuDevsTimeoutException after 3600 seconds
JsonNode finished = client.waitForEval(
        caseId,
        started.path("evalId").asLong(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(3600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
for (JsonNode pair : finished.path("report").path("comparisons")) {
    System.out.println(pair.path("baseline") + " " + pair.path("candidate") + " " + pair.path("target"));
}
```

The example reads the report's paired comparison between the active memo and the memo with the proposed entry. The CLI has no command for this call.

Use it before approving an instruction. The entry stays `proposed` until a person approves it, and the evaluation changes nothing.

## Grouping

Holdout accuracy depends on what the classifier can see. A held-out record is left out of the evidence, but its near-duplicates are not, unless the evaluation also leaves them out. Records that are variants of one product (a changed size or colour, a re-listing, a copy of a template) carry the same label and nearly the same text. The classifier then finds the answer in the evidence and copies it, and the score measures recall of near-copies rather than classification of new records. An ungrouped sample can read far higher than the same setup does on records it has never seen.

So the grouping decides which records leave the evidence together with a sampled record:

- **`nearDuplicate` (default).** For each sampled record, the platform finds its nearest neighbours in the knowledge base by text similarity and leaves out every neighbour whose cosine similarity is at least `threshold` (default 0.98, above 0 and at most 1). Each record is looked up once, so the cost grows with the sample size. Use a lower threshold to group more loosely, and a higher one to group only close copies. A lower threshold such as 0.93 can leave out evidence for distinct products and lower accuracy.
- **`field`.** `groupField` names a record metadata key that identifies one product, such as a base product id or a normalised name. Every record that shares its value is held out together. This is the explicit choice when your data has a reliable key.
- **`none`.** Ungrouped. The report carries the `ungrouped` warning. Read such a run only for its paired difference between variants, as a sign of direction, and do not quote its absolute accuracy.

Both variants of one evaluation share the sample and the leave-out list, so their comparison is always on the same grouping. A comparison between two evaluations needs the same grouping; otherwise the answer is `409`.

The report states the grouping it used (`sample.grouping`): the mode, the field or the threshold, and `groupMembersExcluded`, the number of records the grouping added to the leave-out list.

## Cost

Each variant is a classify job, so a holdout run costs one classify run per variant. An agent that starts a costly evaluation is asked to confirm it above a platform threshold of estimated tokens (see [Agent actions](./agent-actions.md)).
