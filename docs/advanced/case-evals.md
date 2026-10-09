---
title: Holdout evaluation and memo A/B
description: Measure a classification setup against held-out labelled records, compare two runs with paired differences, and test a proposed memo instruction before it is approved.
sidebar_position: 11
---

# Holdout evaluation and memo A/B

A holdout evaluation tells you how well a classification setup would have done on records whose labels you already know. It takes a **grouped sample** of labelled records, classifies each sampled record once per variant with the held-out records left out of the evidence, and scores the results against the historical labels.

Use it to answer questions such as: does a new memo instruction help, does a different tree version or candidate strategy do better, does a different model tier change accuracy. The evaluation is **evidence for the reviewer**. It does not approve anything, and approval stays a human decision in the [case memo](./case-memo.md).

## How a holdout run works

1. **Sample.** The evaluation draws `sampleSize` labelled records (10 to 5,000, default 200). With `groupField` the split is grouped: every record that shares the group value is held out together. Without it, near-duplicate records can stay in the evidence (see the caveat below). The sample seed is stored, so runs with the same seed are comparable.
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
  "groupField": "base_product_id",
  "config": {"llmLevel": "MINI", "targetDepth": [4, 5]},
  "variants": [
    {"name": "baseline"},
    {"name": "with-rule", "memoInclude": ["m_88"]}
  ]
}
```

| Field | Meaning |
|-------|---------|
| `variants` | At least one. `name` is unique within the evaluation, up to 64 characters. |
| `variants[].memoInclude` | Memo entry ids, proposed ones included, loaded on top of the active memo for that variant. |
| `variants[].config` | Classify settings for this variant. It replaces the shared `config` when present. |
| `config` | Classify settings shared by the variants. |

The evaluation pins the effective target depth in its spec, so every variant reports the same levels.

`GET /cases/{caseId}/evals/{evalId}` returns the status, each variant's classify job and status, the sample hash, the records table version, the tree version and the memo version, and, once it completes, the `report`. The report has `sample` (size, requested size, seed, group field, sample hash, excluded records), `levels`, `variants` (score, failed rows, usage, leave-out), and `comparisons` with paired differences per level.

## Compare two runs

`POST /cases/{caseId}/evals:compare` scores two completed evaluations against each other without classifying again.

```json
{"baselineEvalId": 301, "candidateEvalId": 302, "baselineVariant": "baseline", "candidateVariant": "with-rule"}
```

The samples must match: if the two evaluations drew different samples, the answer is `409`. Each comparison reports, per level, the rows newly right, the rows newly wrong and a McNemar exact p-value. It does not yet check that the two runs used the same target depth, so compare runs that share their `targetDepth`.

## Test a proposed memo entry

`POST /cases/{caseId}/memo/entries/{entryId}/evaluate` is the shortcut for a memo A/B. Run A uses the active memo; run B adds the proposed entry. It takes the same optional `sampleSize`, `seed`, `groupField` and `config`, and it returns `{evalId, status}`.

Use it before approving an instruction. The entry stays `proposed` until a person approves it, and the evaluation changes nothing.

## Caveat: group the sample

**Without `groupField` the sample is not grouped, and accuracy is inflated.** Near-duplicate records of a held-out product (size and colour variants, for example) stay in the evidence, so the correct answer is easy to find. In the P3 live check on MINI, a memo A/B on an ungrouped sample reached 83% accuracy, where the grouped P2 holdout of the same kind reached 30 to 37%.

So:

- Set `groupField` to a record metadata key that identifies one product, such as a base product id or a normalised name, for any number you report.
- Read an ungrouped run only for its paired difference, as a sign of direction. Do not quote its absolute accuracy.
- A memo A/B on 200 sampled records of the live check changed 6 leaf decisions (2 gained, 4 lost) with McNemar p = 0.69. That is no evidence either way at this size.

## Python SDK

```python
variants = [
    {"name": "baseline"},
    {"name": "with-rule", "memo_include": [entry["id"]]},
]

started = await client.start_holdout_eval(
    case_id,
    variants,
    sample_size=200,
    seed=7,
    group_field="base_product_id",
    config={"llmLevel": "MINI"},
)
finished = await client.wait_for_eval(case_id, started["evalId"], timeout=3600)
print(finished["report"]["variants"])

comparison = await client.compare_holdout_evals(
    case_id,
    baseline_eval_id=started["evalId"],
    candidate_eval_id=other_eval_id,
    baseline_variant="baseline",
    candidate_variant="with-rule",
)

memo_eval = await client.evaluate_memo_entry(
    case_id, entry["id"], sample_size=200, seed=7, group_field="base_product_id"
)
```

`wait_for_eval` raises `EvalFailed` when the evaluation fails and `TimeoutError` when it runs longer than `timeout`. `get_holdout_eval` returns the status without waiting.

## CLI

```bash
docudevs eval holdout 42 --variants-file variants.json --sample-size 200 --seed 7 --group-field base_product_id --config-file config.json --wait
docudevs eval status 42 301 --wait
docudevs eval compare 42 301 302 --baseline-variant baseline --candidate-variant with-rule
```

`variants.json` is a list of `{"name", "memo_include"?, "config"?}`; the first entry is the baseline.

## Cost

Each variant is a classify job, so a holdout run costs one classify run per variant. An agent that starts a costly evaluation is asked to confirm it above 400,000 estimated tokens (see [Agent actions](./agent-actions.md)).
