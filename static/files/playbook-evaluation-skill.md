---
name: playbook-evaluation
description: Tune a DocuDevs contract-analysis playbook against gold answers from a spreadsheet or table. Use when asked to import gold results, run or score a playbook evaluation, diagnose failures, edit a playbook from evaluation results, or publish a playbook after evaluation. Needs an organization API key in DOCUDEVS_TOKEN, the playbook's case id and the gold file.
---

# Playbook evaluation

Use this guide to run the evaluation loop from a cold start. You need:

- an organization API key in `DOCUDEVS_TOKEN` (never print it),
- the case id of the playbook (`CASE_ID` below),
- the gold file and the contract files it names.

Install the CLI with `pip install docu-devs-api-client`. The command is `docudevs`.

Do not use `docudevs eval holdout` or `docudevs eval compare`. They belong to records classification evaluations, not to playbooks. Read a playbook evaluation with `docudevs eval status CASE_ID EVALUATION_ID` (the id starts with `evaluation-`), and list a case's evaluations with `docudevs eval list CASE_ID`.

Stop and ask a person when a rule below says so. Do not guess at an answer that only a person can give.

## Step 0: Check the draft

Find the case id with `docudevs knowledge-base list`. Then read the draft and its ids:

```
docudevs knowledge-base contract-analysis get CASE_ID --view draft
docudevs playbook export CASE_ID --view draft
```

From the first output, read `draftRevision` and `profile.processing.llmLevel`. The model level must be `mini` or `default`. If it is anything else, including `HIGH`, stop and ask a person. To change `mini` to `default` for a comparison:

1. Copy the `profile` object of the first output into `profile.json`, and set `processing.llmLevel` to `default`.
2. Save it with a reason, at the `draftRevision` you read:

   ```
   docudevs knowledge-base contract-analysis update CASE_ID --draft-revision DRAFT_REVISION --input profile.json --reason "Set the draft model level to default for a comparison baseline"
   ```

A change of level makes earlier baselines invalid. Rerun every baseline after it.

From the second output, copy the ids exactly. Use `clauses[].id`, `clauses[].redFlags[].id`, `clauses[].requirementLevel`, `facts[].id` and `facts[].valueType`. The mapping file must use these ids. The `parse` value must match the fact's type: `number` for `number` or `integer`, `date` for `date`, and `text` for `string`, `choice` or `boolean`.

## Mapping file reference

The mapping file is YAML or JSON with the same keys. Write it with the person who owns the gold file, and have them check it before the import.

```yaml
source:                          # required
  format: xlsx                   # csv, xlsx or json (JSON: an array of row objects)
  file: gold.xlsx                # optional; must equal the gold file's name
  sheet: Gold                    # xlsx only; required when the workbook has several sheets
contract:                        # required
  column: Contract file          # the cell holds the contract's file name, or a path under the folder
  folder: contracts              # optional; relative to the folder of the gold file
caseName:                        # optional
  column: Supplier               # the test case name; defaults to the contract's file name
clauses:                         # optional list; at least one of clauses or facts is required
  - column: Limitation of liability
    clauseId: liability_cap      # a clause in the playbook; each clauseId appears once
    importance: mandatory        # mandatory (default) or advisory
    values:                      # required, at least one entry; keys are cell texts
      Uncapped:
        verdict: flagged         # satisfactory, mixed, flagged, not_found, does_not_apply or ai_unsure
        redFlags:                # optional
          uncapped_liability: triggered   # triggered or not_triggered
facts:                           # optional list
  - column: Notice period (days)
    factId: notice_period_days   # a fact in the playbook; each factId appears once
    parse: number                # optional; number, date or text; must agree with the fact's valueType
    values:                      # optional; maps cell texts to the stored value
      Short: 30
skipValues:                      # optional; cell texts to ignore
  - n/a
skipRows:                        # optional; data row numbers to ignore
  - 4
```

- `sheet` applies only to `format: xlsx`. The import rejects it with any other format.
- `verdict` may be a list, such as `[flagged, mixed]`, when a reviewer accepts more than one verdict. The worked example shows it.
- A blank cell is skipped, unless its blank text is mapped.
- `skipRows` counts the data rows from 1, after the header. Blank rows do not count.
- Every value in a clause column must appear under `values`, or be listed in `skipValues`. Otherwise the dry run fails.
- Unknown top-level keys are rejected. The dry run also rejects a clause or fact id that the playbook does not have, and a red flag that the clause does not have.
- The import cannot tell what `Missing` means in your table. Check each mapping yourself.

## The loop

### 1. Import gold

Write `mapping.yaml` with the ids from step 0, and have a person check it. Then run a dry run:

```
docudevs eval import CASE_ID --gold gold.xlsx --mapping mapping.yaml --dry-run
```

The dry run prints the counts, the contracts to upload, and the skipped cells with their reasons. It stops with an error, naming the row and the value, at the first value it cannot map. Fix the mapping until the dry run passes and every skipped cell is one you meant. Then import:

```
docudevs eval import CASE_ID --gold gold.xlsx --mapping mapping.yaml
```

The import uploads each contract that is not yet in the case (matched by file name), and writes approved expectations recorded as your key. A missing or ambiguous file name fails the dry run.

### 2. Split once

```
docudevs eval split CASE_ID --holdout 0.3 --seed 1
```

Each test case becomes `tuning` or `holdout`. Every later run uses that split. A split creates a new suite revision, so earlier sign-offs go stale. Split again only after a new import, and never between the runs you compare. Cases added later have no set until the next split.

A small suite can end with an empty holdout set. If `docudevs eval run CASE_ID --set holdout` fails with `EMPTY_SET`, stop the loop and report it to a person. Do not count the holdout check as passed.

### 3. Run a baseline

Small set first, then the full set, then the holdout set:

```
docudevs eval run CASE_ID --set tuning --limit 3
docudevs eval run CASE_ID --set tuning
docudevs eval run CASE_ID --set holdout
```

`--limit` takes the first N cases of the set in suite order. Each run prints its evaluation id. Keep the ids of the baseline runs: every later comparison uses them.

`--repeats N` (1 to 3) runs each case N times. Use `--repeats 3` on a baseline or a rerun to find expectations whose result changes between runs. The report lists them under Unstable (see step 4).

A new run is refused with `TRIAL_RUNNING` while a trial of a selected case is still pending. That happens after a `--no-wait` run. Wait for the earlier evaluation to finish, then start the new run.

Do not pass `--llm-level` here. Set the model level in step 0, before the first baseline. To check a run, use `docudevs eval status CASE_ID EVALUATION_ID`.

For a long run, add `--no-wait` and poll the report until `complete` is true:

```
docudevs eval report CASE_ID EVALUATION_ID --format json
```

### 4. Report

```
docudevs eval report CASE_ID EVALUATION_ID
docudevs eval report CASE_ID EVALUATION_ID --format json
```

The Markdown form is for the person. The JSON form is for you. Read these fields first:

- `complete`: when false, some trials have not finished. Do not diagnose yet.
- `contracts[].trialStatus`: `finished`, `failed` or `missing`. A `failed` or `missing` contract with its `failureReason` is not a playbook failure. Rerun the evaluation before you diagnose.
- `totals`: `assertions`, `passed`, `failed`, `unsure` (failed assertions where the actual verdict was `ai_unsure`), `failedContracts`, `missedProblems` and `falseAlarms`. A failed clause assertion is a missed problem when its accepted verdicts (`acceptedVerdicts` if the expectation has them, otherwise `expectedVerdict`) include `flagged` or `mixed`, none of them is `satisfactory`, `not_found` or `does_not_apply`, and the finding gave one of those three or no verdict. It is a false alarm in the reverse case: the accepted verdicts include a clean verdict and none of `flagged` or `mixed`, and the finding gave `flagged` or `mixed`. Passed assertions and fact assertions count in neither. `missedProblems` is the figure the publish check uses (step 8).
- `clauses`, `redFlags`, `facts`: pass rates per clause, red flag agreement and fact matches. `clauses[].verdictPairs` counts each expected-to-actual verdict pair, such as `flagged→satisfactory`; a missing actual verdict counts as `none`.
- `contracts[].assertions[]`: one per expectation. Each has `expectationId`, `passed` and `reasonCode`. A clause assertion has `clauseId`, `expectedVerdict`, `actualVerdict`, `redFlagResults`, `explanation` (the finding's reasoning), `redFlagReasons` (a reason for each red flag outcome, by red flag id), `acceptedVerdicts` (when the expectation has them) and `observedEvidence`: the passages the finding cited, with `quote`, `filename` and `pageNumbers`. A fact assertion has `factId`, `expectedValue` and `actualValue`, and no evidence.
- `reasonCode` on a failed clause assertion is `verdict_mismatch`, `red_flag_mismatch`, `red_flag_not_found` or `clause_not_found`. On a fact it is `fact_mismatch` or `fact_not_found`.
- `comparison` (when you pass `--against`): lists `newlyPassing`, `newlyFailing`, `added` and `removed` (an empty list is left out), and `unchanged`, the number of expectations with the same result.
- `unstable`: expectations whose finished runs disagree, with `passedRuns` of `runs`. It is empty unless the evaluation ran with `--repeats` above 1. The Markdown report shows it as the Unstable section.

### 5. Diagnose

For each assertion with `passed` false, read the finding. The trial id is `contracts[].trialId` in the report:

```
docudevs knowledge-base contract-analysis trial-get CASE_ID TRIAL_ID
docudevs knowledge-base contract-analysis run-result RUN_ID
```

`trial-get` gives the run id (`runId`). In `run-result`, find the clause in `clauses` by `clauseId`. It has `verdict`, `reason`, `explanation` (the finding's reasoning), `redFlags` (each with `outcome` and `rejectedReason`) and `passages` (the quotes). For a fact, read `facts` in the same result: each has `value` and `sourceRefs` with the quotes.

Classify each failure as exactly one of these four classes:

| Class | How to recognise it | What to do |
| --- | --- | --- |
| Playbook wording | A verdict or red-flag mismatch. The `explanation` applies a condition, identification hint or red-flag description from the playbook, and the passage supports the expected answer. The same wording mistake appears in more than one contract. | Propose a wording edit. Edit the playbook, not the gold. |
| Gold looks wrong | The passage quote says clearly the opposite of the expected value or verdict. The finding agrees with the quote. | Report it for a person, with the case id, contract, expectation and quote. Never change the gold. |
| Model variance | The same expectation flips between two runs with the same draft and suite revision. It appears in `newlyPassing` or `newlyFailing` when you compare two runs with no playbook edit between them. | Rerun the set with `--repeats 3`. If the expectation is in the `unstable` list, record it in your notes. Do not edit for it. |
| Engine limit | `clause_not_found` or `fact_not_found`, where the contract text in the case document states the answer. Read the contract text for that section before you decide. Or the `explanation` says the playbook format cannot express the rule. | Report it to DocuDevs with the case id, evaluation id and clause or fact id. Do not edit the playbook for it. |

A finding can look like two classes at first. Use the quote to decide. If the quote supports the expected answer and the finding applied the wording wrongly, it is playbook wording. If the quote supports the finding, it is the gold.

### 6. Edit

Edit the draft with `docudevs playbook export` and `docudevs playbook import`. Every save takes `--reason`: one sentence, as in the rules. Read the draft revision first with `docudevs knowledge-base contract-analysis get CASE_ID --view draft`. This example adds a reason to a red flag's `whyProblematic` field:

```
docudevs playbook export CASE_ID --view draft > playbook.json
```

In `playbook.json`, find the clause `liability_cap`, its red flag `uncapped_liability`, and set `whyProblematic` to the new wording. Then save it at the revision you read:

```
docudevs playbook import CASE_ID playbook.json --draft-revision DRAFT_REVISION --reason "Clarified why an uncapped liability clause is a problem, for failures 3, 5 and 8"
```

The red flag fields are `id`, `flag`, `severity`, `triggeredBy`, `whereApplies`, `whyProblematic`, `whenTriggered`, `favours`, `authorship` and `sourceRefs`. The server rejects any other field with `422`, so take names from the export.

Change one clause or one red flag per save when you can, so each reason matches one fix. A `409` means someone saved the draft since you read it. Read the draft again and apply the change to the new revision.

Read the change log with `list_draft_revisions(CASE_ID)`, which returns `revisions` with `actorId` and `reason`. Read an earlier playbook with `get_draft_revision(CASE_ID, REVISION)`; its `profile` holds that playbook.

### 7. Check on held-back cases

After each edit, rerun the tuning set and then the holdout set. Compare each with the baseline of the same set, and pass the same `--repeats` that the baseline used (1 if it used none):

```
docudevs eval run CASE_ID --set tuning --repeats N
docudevs eval run CASE_ID --set holdout --repeats N
docudevs eval report CASE_ID NEW_EVALUATION_ID --against BASELINE_EVALUATION_ID --format md
```

Both runs of a comparison must use the same `--repeats`. A run with more repeats has more assertions and more passes, so its raw counts are not comparable with the baseline's.

Keep the edit only when the holdout comparison has no `newlyFailing` and the holdout pass rate is not lower than the baseline's. The pass rate is `totals.passed` divided by `totals.assertions` in each report, not the raw `totals.passed`. Otherwise save the earlier playbook again with a reason that says it was reverted and why. Do not compare a tuning run with a holdout run.

If the holdout set is empty, stop (see step 2).

### 8. Publish

Publishing checks the latest run of every case that has a mandatory expectation, against the draft as it is now. Your last save is the final draft. If you save again, rerun step 7 before you go on.

1. **Run the full set on the final draft.** Only trials of the current draft and suite count:

   ```
   docudevs eval run CASE_ID --set all
   docudevs eval report CASE_ID EVALUATION_ID --format json
   ```

   Wait until `complete` is true.
2. **Check the results.** A mandatory expectation that still fails blocks publishing. Stop here, do not attest, and report it. An unstable expectation or a suspected gold error needs a person to decide how to resolve it, for example by correcting the gold and re-importing. The agent does not change the gold or the test suite to get past it.
3. **Check scenario coverage.** Readiness lists `requiredScenarios`, `coveredScenarios` and `outstandingScenarios`. See the scenario section below. If any scenario is outstanding, stop and ask a person for more gold cases.
4. **Attest each passing trial.** Take each trial id from `contracts[].trialId` in the full-set report. Read its `reportId`, then sign off:

   ```
   docudevs knowledge-base contract-analysis trial-get CASE_ID TRIAL_ID
   docudevs knowledge-base contract-analysis trial-attest CASE_ID TRIAL_ID --report-id REPORT_ID --reason "Full-set evaluation EVALUATION_ID, draft revision DRAFT_REVISION"
   ```

   `REPORT_ID` is `report.reportId` in the `trial-get` output. Only a passing scored trial can be attested. `trial-attest` prints the `attestationId`. An attestation counts only for the draft it was made on. Any save or rerun after it needs a new attestation.

5. **Check readiness.** Stop unless `ready` is true:

   ```
   docudevs knowledge-base contract-analysis readiness CASE_ID
   ```

   The output also gives `draftRevision` and `trialAttestationId`. Blockers such as `MANDATORY_CASES_INCOMPLETE` (a mandatory case has no current passing attested trial), `ATTESTATION_STALE` (an attestation no longer matches the draft or suite), `NO_CONFIRMED_EXAMPLES` and `SCENARIO_COVERAGE_MISSING` each need the step that fixes them. The contract-analysis playbooks guide lists the rest.
6. **Compare with the last published draft.** On the full-set evaluation from item 1, run:

   ```
   docudevs eval report CASE_ID EVALUATION_ID --against published --format md
   ```

   Publish only when all of these hold:

   - `comparison.againstSuiteRevision` equals the report's `suiteRevision`. If they differ, stop and ask a person.
   - `comparison.newlyFailing` is empty.
   - Every expectation in `comparison.added` passes. Find each one in `contracts[]` by `caseId`, then check its `assertions[]` by `expectationId`.
   - The missed-problem rate does not rise. The rate is `totals.missedProblems` divided by the sum of `clauses[].assertions`. Compare this report with the baseline's, which you read with `docudevs eval report CASE_ID BASELINE_EVALUATION_ID --format json`.

   The baseline's id is `comparison.againstEvaluationId`. Check in `docudevs eval list CASE_ID` that its `set` is `all`. If it is not, or `NO_PUBLISHED_EVALUATION` comes back, stop and ask a person. A playbook published before this release has no recorded published draft revision, so it gets `NO_PUBLISHED_EVALUATION` until it is published again. Do not publish on an unknown baseline.
7. **Publish** with the values from readiness:

   ```
   docudevs knowledge-base contract-analysis publish CASE_ID --draft-revision DRAFT_REVISION --trial-attestation-id ATTESTATION_ID
   ```

The contract-analysis playbooks guide has the full publish section ("Test and publish a playbook").

## Scenario coverage

Publishing needs all three scenarios covered by approved mandatory expectations on attested passing trials. Each scenario needs gold from a contract of that kind:

- `meets_playbook`: a mandatory clause expectation with `expectedVerdict` `satisfactory`. Gold: a contract where the clause meets the playbook.
- `deviation`: a mandatory clause expectation with `expectedVerdict` `mixed` or `flagged`, and at least one expected red flag set to `triggered`. Gold: a contract with the deviating clause and its red flag.
- `missing_required_clause`: a mandatory clause expectation with `expectedVerdict` `flagged` or `not_found`, no expected red flags, and a clause whose `requirementLevel` is `mandatory` or `expected`. Gold: a contract where the required clause is missing.

If a scenario is outstanding, the fix is more gold from a person. Do not invent expectations, and do not edit existing ones, to cover it.

The worked example below covers all three, provided `liability_cap` has a `requirementLevel` of `mandatory` or `expected`.

## Rules

- Never edit gold. Report a suspected gold error to a person. Never change the gold file or the mapping to make a number better.
- Small set first: `--limit` on tuning, then the full tuning set, then holdout.
- Run baselines on both sets before the first edit.
- Check the holdout before you keep an edit. Tuning results alone prove nothing.
- A reason on every draft save: one sentence, at most 500 characters, naming the failures the change targets. If the change touches an item a person wrote or edited, say so in the reason.
- Use only the CLI and SDK commands in this guide. `docudevs playbook import` and `docudevs knowledge-base contract-analysis update` replace the draft. Both take `--reason`; pass one on every save.
- Do not pass `--llm-level` to `eval run`. The run level comes from the draft's processing settings. Keep it at `mini`, or set `default` with `--reason` (step 0).
- No contract-specific strings or domain words in the playbook. Do not copy party names, amounts, dates, places, or phrases and domain words from a test contract into a clause, red flag, hint or condition. Describe the issue in general terms that would apply to any contract of that kind.
- Publish only when every condition in step 8.6 holds.
- Stop and ask a person when: the draft model level is not `mini` or `default`; the holdout set is empty; the baseline for publishing is unknown; a scenario is outstanding; or a mandatory expectation still fails.
- Do not print the API key, and do not paste contract text into a playbook.

## Worked example: supplier agreements

Gold file `gold.csv`, one row per supplier agreement. The column names are your own:

```csv
Contract file,Supplier,Limitation of liability,Notice period (days),Signed on
supplier-a.pdf,Acme Supply,Capped at 12 months fees,30,2025-03-01
supplier-b.pdf,Beta Logistics,Uncapped,90,2025-06-15
supplier-c.pdf,Gamma Print,Missing,n/a,2024-11-02
```

The playbook has a clause `liability_cap` with `requirementLevel` `mandatory`, a red flag `uncapped_liability`, and facts `notice_period_days` (valueType `number`) and `signed_on` (valueType `date`). The mapping is:

```yaml
source:
  format: csv
  file: gold.csv
contract:
  column: Contract file
  folder: contracts
caseName:
  column: Supplier
clauses:
  - column: Limitation of liability
    clauseId: liability_cap
    importance: mandatory
    values:
      Capped at 12 months fees:
        verdict: satisfactory
      Uncapped:
        verdict: flagged
        redFlags:
          uncapped_liability: triggered
      Missing:
        verdict: not_found
facts:
  - column: Notice period (days)
    factId: notice_period_days
    parse: number
  - column: Signed on
    factId: signed_on
    parse: date
skipValues:
  - n/a
```

What the import does with it:

- `Contract file` names the contract. `supplier-a.pdf` is uploaded from the `contracts` folder next to the gold file, unless it is already in the case.
- `Supplier` names each test case.
- `Capped at 12 months fees` becomes a `satisfactory` expectation for `liability_cap`. `Uncapped` becomes `flagged` with `uncapped_liability` triggered. `Missing` becomes `not_found`.
- `verdict` may be a list, such as `verdict: [flagged, mixed]`, when a reviewer accepts more than one verdict. The first entry is the expected verdict, the whole list is sent as `acceptedVerdicts`, and an actual verdict from the list passes. An empty list is a mapping error.
- `Notice period (days)` becomes a number fact. `n/a` is skipped, as listed in `skipValues`.
- `Signed on` becomes a date fact, which must be `YYYY-MM-DD`.

The example covers the three scenarios: `supplier-a` is `meets_playbook`, `supplier-b` is `deviation`, and `supplier-c` is `missing_required_clause`.

The import cannot tell that `Missing` means `not_found`. Check each value in the mapping yourself, because a wrong mapping does not fail the import. It only changes the score.

If the playbook lacks `liability_cap` or its `uncapped_liability` red flag, the dry run fails. Check the ids in step 0 first.

## Worked example: a failure, end to end

1. `eval report` shows `supplier-b` with `liability_cap` expected `flagged` and actual `satisfactory`, reason `verdict_mismatch`.
2. `trial-get` gives the run; `run-result` shows the `liability_cap` finding. Its `explanation` says the liability is capped at 12 months' fees, and the passage quote reads "liability is capped at the total fees paid in the last 12 months".
3. The quote supports the finding and contradicts the gold, which expects an uncapped liability. This is a gold problem. Report it to a person with the contract, expectation and quote, and do not edit the gold. Leave the playbook alone for this failure.
4. Another contract, `supplier-d`, shows a different failure on the same clause. The `explanation` treats a cap that covers only indirect damages as a full liability cap. The quote supports the expected `flagged` verdict. The red flag wording does not say that a partial cap does not count, so this is playbook wording. Propose a clearer `whyProblematic` for the red flag, save it with a reason that names the failures it targets, then rerun tuning and holdout.
