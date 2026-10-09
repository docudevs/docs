---
title: Holdout evaluation and memo A/B
description: Measure a classification setup against held-out labelled records, compare two runs with paired differences, and test a proposed memo instruction before it is approved.
sidebar_position: 11
---

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
| `variants` | At least one. `name` is unique within the evaluation, up to 64 characters. |
| `variants[].memoInclude` | Memo entry ids, proposed ones included, loaded on top of the active memo for that variant. |
| `variants[].config` | Classify settings for this variant. It replaces the shared `config` when present. |
| `config` | Classify settings shared by the variants. |
| `grouping` | How near-duplicates of the sample are kept out of its evidence. See [Grouping](#grouping). Omitted means `nearDuplicate` at the default threshold. |
| `groupField` | A record metadata key. Records that share its value are held out together. Cannot be combined with `grouping`, unless `grouping.mode` is `field`. |

The evaluation pins the effective target depth in its spec, so every variant reports the same levels.

`GET /cases/{caseId}/evals/{evalId}` returns the status, each variant's classify job and status, the sample hash, the records table version, the tree version and the memo version, and, once it completes, the `report`. The report has `sample` (size, requested size, seed, group field, grouping, sample hash, excluded records), `levels`, `variants` (score, failed rows, usage, leave-out), `comparisons` with paired differences per level, and `warnings`. A run with grouping `none` carries the warning `ungrouped`.

## Compare two runs

`POST /cases/{caseId}/evals:compare` scores two completed evaluations against each other without classifying again.

```json
{"baselineEvalId": 301, "candidateEvalId": 302, "baselineVariant": "baseline", "candidateVariant": "with-rule"}
```

The samples and the grouping must match: if the two evaluations drew different samples, or used a different grouping mode, field or threshold, the answer is `409`. Each comparison reports, per level, the rows newly right, the rows newly wrong and a McNemar exact p-value. It does not yet check that the two runs used the same target depth, so compare runs that share their `targetDepth`.

## Test a proposed memo entry

`POST /cases/{caseId}/memo/entries/{entryId}/evaluate` is the shortcut for a memo A/B. Run A uses the active memo; run B adds the proposed entry. It takes the same optional `sampleSize`, `seed`, `groupField`, `grouping` and `config`, and it returns `{evalId, status}`.

Use it before approving an instruction. The entry stays `proposed` until a person approves it, and the evaluation changes nothing.

## Grouping

Holdout accuracy depends on what the classifier can see. A held-out record is left out of the evidence, but its near-duplicates are not, unless the evaluation also leaves them out. Records that are variants of one product (a changed size or colour, a re-listing, a copy of a template) carry the same label and nearly the same text. The classifier then finds the answer in the evidence and copies it, and the score measures recall of near-copies rather than classification of new records. An ungrouped sample can read far higher than the same setup does on records it has never seen.

So the grouping decides which records leave the evidence together with a sampled record:

- **`nearDuplicate` (default).** For each sampled record, the platform finds its nearest neighbours in the knowledge base by text similarity and leaves out every neighbour whose cosine similarity is at least `threshold` (default 0.98, above 0 and at most 1). Each record is looked up once, so the cost grows with the sample size. Use a lower threshold to group more loosely, and a higher one to group only close copies. The default was calibrated on a live labelled sample on 2026-10-09: a lower threshold such as 0.93 left out evidence for distinct products and lowered accuracy.
- **`field`.** `groupField` names a record metadata key that identifies one product, such as a base product id or a normalised name. Every record that shares its value is held out together. This is the explicit choice when your data has a reliable key.
- **`none`.** Ungrouped. The report carries the `ungrouped` warning. Read such a run only for its paired difference between variants, as a sign of direction, and do not quote its absolute accuracy.

Both variants of one evaluation share the sample and the leave-out list, so their comparison is always on the same grouping. A comparison between two evaluations needs the same grouping; otherwise the answer is `409`.

The report states the grouping it used (`sample.grouping`): the mode, the field or the threshold, and `groupMembersExcluded`, the number of records the grouping added to the leave-out list.

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
    grouping={"mode": "nearDuplicate", "threshold": 0.98},
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
docudevs eval holdout 42 --variants-file variants.json --sample-size 200 --seed 7 --grouping-mode nearDuplicate --grouping-threshold 0.98 --config-file config.json --wait
docudevs eval holdout 42 --variants-file variants.json --sample-size 200 --seed 7 --group-field base_product_id --config-file config.json --wait
docudevs eval status 42 301 --wait
docudevs eval compare 42 301 302 --baseline-variant baseline --candidate-variant with-rule
```

`variants.json` is a list of `{"name", "memo_include"?, "config"?}`; the first entry is the baseline.

## Cost

Each variant is a classify job, so a holdout run costs one classify run per variant. An agent that starts a costly evaluation is asked to confirm it above 400,000 estimated tokens (see [Agent actions](./agent-actions.md)).
