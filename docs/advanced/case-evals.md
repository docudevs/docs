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
| `variants` | One to four. `name` is unique within the evaluation, up to 64 characters. |
| `variants[].memoInclude` | Memo entry ids, proposed ones included, loaded on top of the active memo for that variant. |
| `variants[].config` | Classify settings for this variant. It replaces the shared `config` when present. |
| `config` | Classify settings shared by the variants. |
| `grouping` | How near-duplicates of the sample are kept out of its evidence. See [Grouping](#grouping). Omitted means `nearDuplicate` at the default threshold. |
| `groupField` | A record metadata key. Records that share its value are held out together. Cannot be combined with `grouping`, unless `grouping.mode` is `field`. |

The evaluation pins the effective target depth in its spec, so every variant reports the same levels.

`GET /cases/{caseId}/evals/{evalId}` returns the status, each variant's classify job and status, the sample hash, the records table version, the tree version and the memo version, and, once it completes, the `report`. The report has `sample` (size, requested size, seed, group field, grouping, sample hash, excluded records), `levels`, `variants` (score, failed rows, usage, leave-out), `comparisons` with paired differences per level, and `warnings`. A run with grouping `none` carries the warning `ungrouped`.

An evaluation is asynchronous. Its `status` moves through `PREPARING`, `RUNNING` and `SCORING` to `COMPLETED` or `FAILED`; the `report` is present once it is `COMPLETED`, and `error` says why a run `FAILED`.

<Example name="case-evals/run-holdout-eval" />

The example starts the evaluation, waits for it and prints each variant's accuracy at the target depth. The Python SDK takes `memo_include` in a variant and `sample_size` and `group_field` as keyword arguments, where the request uses `memoInclude`, `sampleSize` and `groupField`; the CLI reads `variants.json` as a list of `{"name", "memo_include"?, "config"?}` and its first entry is the baseline. To group by a record metadata key, pass `groupField` (`group_field` in Python, `--group-field` in the CLI) instead of the grouping options. `get_holdout_eval` in Python and `docudevs eval status` read the status without waiting.

## Compare two runs

`POST /cases/{caseId}/evals:compare` scores two completed evaluations against each other without classifying again. It answers `202` with `{evalId, jobGuid, status}`: the comparison is a new evaluation with status `SCORING`, and you read its `report.comparisons` from `GET /cases/{caseId}/evals/{evalId}` once it is `COMPLETED`.

```json
{"baselineEvalId": 301, "candidateEvalId": 302, "baselineVariant": "baseline", "candidateVariant": "with-rule"}
```

<Example name="case-evals/compare-evals" />

The samples and the grouping must match: if the two evaluations drew different samples, or used a different grouping mode, field or threshold, the answer is `409`. Each comparison reports, for the target level and for every level in `byLevel`, the rows right in both runs (`bothRight`), right only in the baseline (`aOnly`), right only in the candidate (`bOnly`) and wrong in both (`bothWrong`), with a McNemar exact p-value (`pValue`). It does not check that the two runs used the same target depth, so compare runs that share their `targetDepth`.

## Test a proposed memo entry

`POST /cases/{caseId}/memo/entries/{entryId}/evaluate` is the shortcut for a memo A/B. Run A uses the active memo; run B adds the proposed entry. It takes the same optional `sampleSize`, `seed`, `groupField`, `grouping` and `config`, and it returns `202` with `{evalId, jobGuid, status}`, like a holdout evaluation. It answers `409` when the entry is not a proposed instruction.

<Example name="case-evals/evaluate-memo-entry" />

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
