---
title: Playbook evaluation
description: Test a contract-analysis playbook against gold answers from a spreadsheet, diagnose the failures and improve the playbook with a coding agent or by hand.
---
<!-- Generated from docs/core/playbook-evaluation.md. Do not edit by hand. -->

# Playbook evaluation

Use this guide to run the evaluation loop from a cold start. You need:

- an organization API key in `DOCUDEVS_TOKEN` (keep it out of logs and shared output); the cURL, Python and Java examples below read `API_KEY`, so set it to the same key (the CLI reads either variable),
- the case id of the playbook (`CASE_ID` below),
- the gold file and the contract files it names.

Install the CLI with `pip install docu-devs-api-client`. The command is `docudevs`.

Do not use `docudevs eval holdout` or `docudevs eval compare`. They belong to records classification evaluations, not to playbooks. Read a playbook evaluation with `docudevs eval status CASE_ID EVALUATION_ID` (the id starts with `evaluation-`), and list a case's evaluations with `docudevs eval list CASE_ID`.

Where a step says to check with the playbook owner, do that instead of guessing at an answer that only a person can give.

## Use it as an agent skill

The same procedure is available as an agent skill: [playbook-evaluation-skill.md](https://docs.docudevs.ai/files/playbook-evaluation-skill.md). Give the file to a coding agent and ask it to evaluate your playbook, or follow the steps below by hand.

## Step 0: Check the draft

Find the case id with `docudevs knowledge-base list`. Then read the draft and its ids:

```bash
docudevs knowledge-base contract-analysis get CASE_ID --view draft
docudevs playbook export CASE_ID --view draft
```

From the first output, read `draftRevision` and `profile.processing.llmLevel`. The model level must be `mini` or `default`. If it is anything else, for example `HIGH`, stop and check with the playbook owner before you go on. To change `mini` to `default` for a comparison:

1. Copy the `profile` object of the first output into `profile.json`, and set `processing.llmLevel` to `default`.
2. Save it with a reason, at the `draftRevision` you read:

   ```bash
   docudevs knowledge-base contract-analysis update CASE_ID --draft-revision DRAFT_REVISION --input profile.json --reason "Set the draft model level to default for a comparison baseline"
   ```

A change of level makes earlier baselines invalid. Rerun every baseline after it.

From the second output, copy the ids exactly. Use `clauses[].id`, `clauses[].redFlags[].id`, `clauses[].requirementLevel`, `facts[].id` and `facts[].valueType`. The mapping file must use these ids. The `parse` value must match the fact's type: `number` for `number` or `integer`, `date` for `date`, and `text` for `string`, `choice` or `boolean`.

## Mapping file reference

The mapping file is YAML or JSON with the same keys. Write it with the person who owns the gold file, and ask them to check it before the import.

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

Write `mapping.yaml` with the ids from step 0, and ask the gold file's owner to check it. Then run a dry run:

```bash
docudevs eval import CASE_ID --gold gold.xlsx --mapping mapping.yaml --dry-run
```

The dry run prints the counts, the contracts to upload, and the skipped cells with their reasons. It stops with an error, naming the row and the value, at the first value it cannot map. Fix the mapping until the dry run passes and every skipped cell is one you meant. Then import:

```bash
docudevs eval import CASE_ID --gold gold.xlsx --mapping mapping.yaml
```

The import uploads each contract that is not yet in the case (matched by file name), and writes approved expectations recorded as your key. A missing or ambiguous file name fails the dry run.

### 2. Split once

```bash
docudevs eval split CASE_ID --holdout 0.3 --seed 1
```

Each test case becomes `tuning` or `holdout`. Every later run uses that split. A split creates a new suite revision, so earlier sign-offs go stale. Split again only after a new import, and never between the runs you compare. Cases added later have no set until the next split.

A small suite can end with an empty holdout set. If `docudevs eval run CASE_ID --set holdout` fails with `EMPTY_SET`, stop the loop and tell the playbook owner. Do not count the holdout check as passed.

### 3. Run a baseline

Small set first, then the full set, then the holdout set:

```bash
docudevs eval run CASE_ID --set tuning --limit 3
docudevs eval run CASE_ID --set tuning
docudevs eval run CASE_ID --set holdout
```

`--limit` takes the first N cases of the set in suite order. Each run prints its evaluation id. Keep the ids of the baseline runs: every later comparison uses them.

`--repeats N` (1 to 3) runs each case N times. Use `--repeats 3` on a baseline or a rerun to find expectations whose result changes between runs. The report lists them under Unstable (see step 4).

A new run is refused with `TRIAL_RUNNING` while a trial of a selected case is still pending. That happens after a `--no-wait` run. Wait for the earlier evaluation to finish, then start the new run.

Do not pass `--llm-level` here. Set the model level in step 0, before the first baseline. To check a run, use `docudevs eval status CASE_ID EVALUATION_ID`.

For a long run, add `--no-wait` and poll the report until `complete` is true:

```bash
docudevs eval report CASE_ID EVALUATION_ID --format json
```

### 4. Report

```bash
docudevs eval report CASE_ID EVALUATION_ID
docudevs eval report CASE_ID EVALUATION_ID --format json
```

The Markdown form is for reading. The JSON form is for scripts and agents. Read these fields first:

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

```bash
docudevs knowledge-base contract-analysis trial-get CASE_ID TRIAL_ID
docudevs knowledge-base contract-analysis run-result RUN_ID
```

`trial-get` gives the run id (`runId`). In `run-result`, find the clause in `clauses` by `clauseId`. It has `verdict`, `reason`, `explanation` (the finding's reasoning), `redFlags` (each with `outcome` and `rejectedReason`) and `passages` (the quotes). For a fact, read `facts` in the same result: each has `value` and `sourceRefs` with the quotes.

Classify each failure as exactly one of these four classes:

| Class | How to recognise it | What to do |
| --- | --- | --- |
| Playbook wording | A verdict or red-flag mismatch. The `explanation` applies a condition, identification hint or red-flag description from the playbook, and the passage supports the expected answer. The same wording mistake appears in more than one contract. | Propose a wording edit. Edit the playbook, not the gold. |
| Gold looks wrong | The passage quote says clearly the opposite of the expected value or verdict. The finding agrees with the quote. | Report it to the gold file's owner, with the case id, contract, expectation and quote. Never change the gold. |
| Model variance | The same expectation flips between two runs with the same draft and suite revision. It appears in `newlyPassing` or `newlyFailing` when you compare two runs with no playbook edit between them. | Rerun the set with `--repeats 3`. If the expectation is in the `unstable` list, note it down. Do not edit the playbook for it. |
| Engine limit | `clause_not_found` or `fact_not_found`, where the contract text in the case document states the answer. Read the contract text for that section before you decide. Or the `explanation` says the playbook format cannot express the rule. | Report it to DocuDevs support with the case id, evaluation id and clause or fact id. Do not edit the playbook for it. |

A finding can look like two classes at first. Use the quote to decide. If the quote supports the expected answer and the finding applied the wording wrongly, it is playbook wording. If the quote supports the finding, it is the gold.

### 6. Edit

Edit the draft with `docudevs playbook export` and `docudevs playbook import`. Every save takes `--reason`: one sentence, as described under Rules. Read the draft revision first with `docudevs knowledge-base contract-analysis get CASE_ID --view draft`. This example adds a reason to a red flag's `whyProblematic` field:

```bash
docudevs playbook export CASE_ID --view draft > playbook.json
```

In `playbook.json`, find the clause `liability_cap`, its red flag `uncapped_liability`, and set `whyProblematic` to the new wording. Then save it at the revision you read:

```bash
docudevs playbook import CASE_ID playbook.json --draft-revision DRAFT_REVISION --reason "Clarified why an uncapped liability clause is a problem, for failures 3, 5 and 8"
```

The red flag fields are `id`, `flag`, `severity`, `triggeredBy`, `whereApplies`, `whyProblematic`, `whenTriggered`, `favours`, `authorship` and `sourceRefs`. The server rejects any other field with `422`, so take names from the export.

Change one clause or one red flag per save when you can, so each reason matches one fix. A `409` means someone saved the draft since you read it. Read the draft again and apply the change to the new revision.

Read the change log with `list_draft_revisions(CASE_ID)`, which returns `revisions` with `actorId` and `reason`. Read an earlier playbook with `get_draft_revision(CASE_ID, REVISION)`; its `profile` holds that playbook.

### 7. Check on held-back cases

After each edit, rerun the tuning set and then the holdout set. Compare each with the baseline of the same set, and pass the same `--repeats` that the baseline used (1 if it used none):

```bash
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

   ```bash
   docudevs eval run CASE_ID --set all
   docudevs eval report CASE_ID EVALUATION_ID --format json
   ```

   Wait until `complete` is true.
2. **Check the results.** A mandatory expectation that still fails blocks publishing. Stop here, do not attest, and report it. An unstable expectation or a suspected gold error needs a person to decide how to resolve it, for example by correcting the gold and re-importing. Do not change the gold or the test suite to get past it.
3. **Check scenario coverage.** Readiness lists `requiredScenarios`, `coveredScenarios` and `outstandingScenarios`. See the scenario section below. If any scenario is outstanding, stop and ask the gold file's owner for more gold cases.
4. **Attest each passing trial.** Take each trial id from `contracts[].trialId` in the full-set report. Read its `reportId`, then sign off:

   ```bash
   docudevs knowledge-base contract-analysis trial-get CASE_ID TRIAL_ID
   docudevs knowledge-base contract-analysis trial-attest CASE_ID TRIAL_ID --report-id REPORT_ID --reason "Full-set evaluation EVALUATION_ID, draft revision DRAFT_REVISION"
   ```

   `REPORT_ID` is `report.reportId` in the `trial-get` output. Only a passing scored trial can be attested. `trial-attest` prints the `attestationId`. An attestation counts only for the draft it was made on. Any save or rerun after it needs a new attestation.

5. **Check readiness.** Stop unless `ready` is true:

   ```bash
   docudevs knowledge-base contract-analysis readiness CASE_ID
   ```

   The output also gives `draftRevision` and `trialAttestationId`. Blockers such as `MANDATORY_CASES_INCOMPLETE` (a mandatory case has no current passing attested trial), `ATTESTATION_STALE` (an attestation no longer matches the draft or suite), `NO_CONFIRMED_EXAMPLES` and `SCENARIO_COVERAGE_MISSING` each need the step that fixes them. The contract-analysis playbooks guide lists the rest.
6. **Compare with the last published draft.** On the full-set evaluation from item 1, run:

   ```bash
   docudevs eval report CASE_ID EVALUATION_ID --against published --format md
   ```

   Publish only when all of these hold:

   - `comparison.againstSuiteRevision` equals the report's `suiteRevision`. If they differ, stop and check with the playbook owner.
   - `comparison.newlyFailing` is empty.
   - Every expectation in `comparison.added` passes. Find each one in `contracts[]` by `caseId`, then check its `assertions[]` by `expectationId`.
   - The missed-problem rate does not rise. The rate is `totals.missedProblems` divided by the sum of `clauses[].assertions`. Compare this report with the baseline's, which you read with `docudevs eval report CASE_ID BASELINE_EVALUATION_ID --format json`.

   The baseline's id is `comparison.againstEvaluationId`. Check in `docudevs eval list CASE_ID` that its `set` is `all`. If it is not, or `NO_PUBLISHED_EVALUATION` comes back, stop and check with the playbook owner. A playbook with no recorded published draft revision returns `NO_PUBLISHED_EVALUATION` until it is published again. Do not publish on an unknown baseline.
7. **Publish** with the values from readiness:

   ```bash
   docudevs knowledge-base contract-analysis publish CASE_ID --draft-revision DRAFT_REVISION --trial-attestation-id ATTESTATION_ID
   ```

The contract-analysis playbooks guide has the full publish section ("Test and publish a playbook").

## Run the loop from Python, Java or cURL

The steps that need no judgement can run from a script. The examples read the key from `API_KEY`, use the base URL `https://api.docudevs.ai`, and work with an organization API key. The Python variants use `DocuDevsClient`; its evaluation methods raise an error when the API answers with an error status. The Java variants use `DocuDevsClient` from `ai.docudevs.client`, which raises `DocuDevsApiException` in the same case. Each subsection does what the CLI step it names does.

### Import gold (step 1)

The client reads the gold table and the mapping file and uploads the contracts itself. Do the dry run first, as in step 1. There is no cURL or Java variant: the client uploads the contracts that are missing, prepares a test package for each and saves the test suite, so the import is several calls, not one route.

**Python**

```python
case_id = 42  # id of the playbook's case

# A dry run reads the files and validates the mapping without writing anything.
# Both calls raise an error that names the row and the value at the first cell that cannot be mapped.
dry_run = await client.import_gold(case_id, "gold.xlsx", "mapping.yaml", dry_run=True)
print(dry_run.summary())

imported = await client.import_gold(case_id, "gold.xlsx", "mapping.yaml")
print(imported.summary())
```

**CLI**

```bash
CASE_ID=42  # id of the playbook's case

docudevs eval import "$CASE_ID" --gold gold.xlsx --mapping mapping.yaml --dry-run
docudevs eval import "$CASE_ID" --gold gold.xlsx --mapping mapping.yaml
```

### Split the suite (step 2)

The split needs the suite revision you read, and answers `409` when someone saved the suite since. The CLI reads the revision itself.

**cURL**

```bash
CASE_ID=42  # id of the playbook's case

REVISION=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $API_KEY" | jq -r '.revision')

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite/split" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson revision "$REVISION" '{holdout: 0.3, seed: 1, expectedSuiteRevision: $revision}')" \
  | jq -r '(.cases // []) as $cases
    | ([$cases[] | select(.set == "tuning")] | length) as $tuning
    | ([$cases[] | select(.set == "holdout")] | length) as $holdout
    | "Suite revision \(.revision) saved: \($tuning) tuning, \($holdout) holdout, \(($cases | length) - $tuning - $holdout) unassigned."'
```

**Python**

```python
case_id = 42  # id of the playbook's case

# Both calls raise when the API answers with an error status
suite = await client.get_contract_analysis_test_suite(case_id)
saved = await client.split_test_suite(
    case_id,
    holdout=0.3,
    seed=1,
    expected_suite_revision=suite.parsed["revision"],
)

cases = saved.parsed.get("cases", [])
tuning = sum(1 for case in cases if case.get("set") == "tuning")
holdout = sum(1 for case in cases if case.get("set") == "holdout")
print(
    f"Suite revision {saved.parsed['revision']} saved: {tuning} tuning, "
    f"{holdout} holdout, {len(cases) - tuning - holdout} unassigned."
)
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

long caseId = 42L; // id of the playbook's case

// Both calls raise DocuDevsApiException when the API answers with an error status
JsonNode suite = client.getContractAnalysisTestSuite(caseId);
JsonNode saved = client.splitTestSuite(
        caseId,
        new ObjectMapper().createObjectNode()
                .put("holdout", 0.3)
                .put("seed", 1)
                .put("expectedSuiteRevision", suite.path("revision").asLong()));

int tuning = 0;
int holdout = 0;
JsonNode cases = saved.path("cases");
for (JsonNode testCase : cases) {
    String set = testCase.path("set").asText();
    if (set.equals("tuning")) {
        tuning++;
    } else if (set.equals("holdout")) {
        holdout++;
    }
}
System.out.println("Suite revision " + saved.path("revision").asLong() + " saved: " + tuning
        + " tuning, " + holdout + " holdout, " + (cases.size() - tuning - holdout) + " unassigned.");
```

**CLI**

```bash
CASE_ID=42  # id of the playbook's case

docudevs eval split "$CASE_ID" --holdout 0.3 --seed 1
```

### Run an evaluation and wait (step 3)

An evaluation starts one scored trial per test case of the set. A start answers `400` with `EMPTY_SET` when the set has no test cases, and `409` with `TRIAL_RUNNING` while a trial of a selected case is still pending. The request also takes `limit` and `repeats`, as the CLI options do. The API needs an idempotency key on a start: the Python client, the Java client and the CLI create one, and the cURL variant sends one. The cURL and Python variants poll every 15 seconds for up to 3600 seconds, and the CLI and the Java `waitForEvaluation` call wait the same way. The wait ends when every trial has finished or failed.

**cURL**

```bash
CASE_ID=42  # id of the playbook's case

wait_for_evaluation() {
  local deadline=$((SECONDS + 3600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$1" \
      -H "Authorization: $API_KEY")
    if [ "$(echo "$STATUS" | jq -r '.complete')" = "true" ]; then
      echo "Evaluation $1 complete: $(echo "$STATUS" | jq -c '.trialCounts // {}')"
      return 0
    fi
    sleep 15
  done
  echo "Evaluation $1 did not complete within 3600 seconds" >&2
  return 1
}

# Send the same key again to retry a start safely
KEY=$(python3 -c 'import uuid; print(uuid.uuid4().hex)')
STARTED=$(curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg key "$KEY" '{set: "tuning", idempotencyKey: $key}')")
EVALUATION_ID=$(echo "$STARTED" | jq -r '.evaluationId')
echo "Evaluation $EVALUATION_ID started: $(echo "$STARTED" | jq '(.caseIds // []) | length') cases"

wait_for_evaluation "$EVALUATION_ID"
```

**Python**

```python
case_id = 42  # id of the playbook's case

# The client creates the idempotency key; pass idempotency_key to retry a start safely
started = await client.start_evaluation(case_id, set="tuning")
evaluation_id = started.parsed["evaluationId"]
print(f"Evaluation {evaluation_id} started: {len(started.parsed.get('caseIds', []))} cases")

# Polls every 15 seconds and raises TimeoutError after 3600 seconds
finished = await client.wait_for_evaluation(
    case_id, evaluation_id, poll_seconds=15, timeout=3600
)
print(f"Evaluation {evaluation_id} complete: {finished.parsed.get('trialCounts', {})}")
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;

long caseId = 42L; // id of the playbook's case

// The client creates the idempotency key; pass one as the last argument to retry a start safely
JsonNode started = client.startEvaluation(caseId, "tuning", null, null);
String evaluationId = started.path("evaluationId").asText();
System.out.println("Evaluation " + evaluationId + " started: " + started.path("caseIds").size() + " cases");

// Polls every 15 seconds and throws DocuDevsTimeoutException after 3600 seconds
JsonNode finished = client.waitForEvaluation(
        caseId,
        evaluationId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(3600))
                .pollInterval(Duration.ofSeconds(15))
                .build());
System.out.println("Evaluation " + evaluationId + " complete: " + finished.path("trialCounts"));
```

**CLI**

```bash
CASE_ID=42  # id of the playbook's case

docudevs eval run "$CASE_ID" --set tuning
```

### Read the status and the report (steps 3 and 4)

The report is the one described in step 4. While `complete` is false the report covers only the trials that have finished, so read the figures after it turns true. `complete` is true when every trial has finished or failed, so check each contract's `trialStatus` for failed trials. Add `against` with an earlier evaluation id, or `published`, to get the comparison from steps 7 and 8. The CLI prints the whole status and report as JSON, and the Python, Java and cURL variants print a summary.

**cURL**

```bash
CASE_ID=42  # id of the playbook's case

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.evaluations // [])[] | "\(.evaluationId) \(.set) \(if .complete then "complete" else "running" end)"'

EVALUATION_ID="<evaluation-id>"  # evaluationId returned when the evaluation was started

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$EVALUATION_ID" \
  -H "Authorization: $API_KEY" | jq -r '"Trials: \(.trialCounts // {} | tojson)"'

# Add ?against=<evaluation id> or ?against=published to compare with another evaluation
curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$EVALUATION_ID/report" \
  -H "Authorization: $API_KEY" \
  | jq -r '"Complete: \(.complete)",
    "Passed \(.totals.passed) of \(.totals.assertions) assertions",
    "Missed problems: \(.totals.missedProblems // 0), false alarms: \(.totals.falseAlarms // 0)",
    ((.contracts // [])[] | select(.trialStatus != "finished") | "\(.name): \(.trialStatus) (\(.failureReason // "no reason"))")'
```

**Python**

```python
case_id = 42  # id of the playbook's case

listing = await client.list_evaluations(case_id)
for evaluation in listing.parsed.get("evaluations", []):
    print(evaluation["evaluationId"], evaluation["set"], "complete" if evaluation["complete"] else "running")

evaluation_id = "<evaluation-id>"  # evaluationId returned when the evaluation was started
status = await client.get_evaluation(case_id, evaluation_id)
print(f"Trials: {status.parsed.get('trialCounts', {})}")

# Pass against=<evaluation id> or against="published" to compare with another evaluation
response = await client.get_evaluation_report(case_id, evaluation_id)
report = response.parsed
totals = report["totals"]
print(f"Complete: {report['complete']}")
print(f"Passed {totals['passed']} of {totals['assertions']} assertions")
print(f"Missed problems: {totals.get('missedProblems', 0)}, false alarms: {totals.get('falseAlarms', 0)}")
for contract in report.get("contracts", []):
    if contract["trialStatus"] != "finished":
        print(f"{contract['name']}: {contract['trialStatus']} ({contract.get('failureReason') or 'no reason'})")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of the playbook's case

JsonNode listing = client.listEvaluations(caseId);
for (JsonNode evaluation : listing.path("evaluations")) {
    System.out.println(evaluation.path("evaluationId").asText() + " " + evaluation.path("set").asText() + " "
            + (evaluation.path("complete").asBoolean() ? "complete" : "running"));
}

String evaluationId = "<evaluation-id>"; // evaluationId returned when the evaluation was started
JsonNode status = client.getEvaluation(caseId, evaluationId);
System.out.println("Trials: " + status.path("trialCounts"));

// Pass an evaluation id or "published" instead of null to compare with another evaluation
JsonNode report = client.getEvaluationReport(caseId, evaluationId, null);
JsonNode totals = report.path("totals");
System.out.println("Complete: " + report.path("complete").asBoolean());
System.out.println("Passed " + totals.path("passed").asLong() + " of " + totals.path("assertions").asLong()
        + " assertions");
System.out.println("Missed problems: " + totals.path("missedProblems").asLong() + ", false alarms: "
        + totals.path("falseAlarms").asLong());
for (JsonNode contract : report.path("contracts")) {
    if (!contract.path("trialStatus").asText().equals("finished")) {
        System.out.println(contract.path("name").asText() + ": " + contract.path("trialStatus").asText()
                + " (" + contract.path("failureReason").asText("no reason") + ")");
    }
}
```

**CLI**

```bash
CASE_ID=42  # id of the playbook's case
# The CLI prints the whole status and report as JSON; the Python and cURL variants print a summary

docudevs eval list "$CASE_ID"

EVALUATION_ID="<evaluation-id>"  # evaluationId returned when the evaluation was started

docudevs eval status "$CASE_ID" "$EVALUATION_ID"

# Add --against <evaluation id> or --against published to compare with another evaluation
docudevs eval report "$CASE_ID" "$EVALUATION_ID" --format json
```

## Scenario coverage

Publishing needs all three scenarios covered by approved mandatory expectations on attested passing trials. Each scenario needs gold from a contract of that kind:

- `meets_playbook`: a mandatory clause expectation with `expectedVerdict` `satisfactory`. Gold: a contract where the clause meets the playbook.
- `deviation`: a mandatory clause expectation with `expectedVerdict` `mixed` or `flagged`, and at least one expected red flag set to `triggered`. Gold: a contract with the deviating clause and its red flag.
- `missing_required_clause`: a mandatory clause expectation with `expectedVerdict` `flagged` or `not_found`, no expected red flags, and a clause whose `requirementLevel` is `mandatory` or `expected`. Gold: a contract where the required clause is missing.

If a scenario is outstanding, the fix is more gold from the gold file's owner. Do not invent expectations, and do not edit existing ones, to cover it.

The worked example below covers all three, provided `liability_cap` has a `requirementLevel` of `mandatory` or `expected`.

## Rules

- Never edit gold. Report a suspected gold error to the gold file's owner. Never change the gold file or the mapping to make a number better.
- Small set first: `--limit` on tuning, then the full tuning set, then holdout.
- Run baselines on both sets before the first edit.
- Check the holdout before you keep an edit. Tuning results alone prove nothing.
- A reason on every draft save: one sentence, at most 500 characters, naming the failures the change targets. If the change touches an item a person wrote or edited, say so in the reason.
- Use only the CLI and SDK commands in this guide. `docudevs playbook import` and `docudevs knowledge-base contract-analysis update` replace the draft. Both take `--reason`; pass one on every save.
- Do not pass `--llm-level` to `eval run`. The run level comes from the draft's processing settings. Keep it at `mini`, or set `default` with `--reason` (step 0).
- No contract-specific strings or domain words in the playbook. Do not copy party names, amounts, dates, places, or phrases and domain words from a test contract into a clause, red flag, hint or condition. Describe the issue in general terms that would apply to any contract of that kind.
- Publish only when every condition in step 8.6 holds.
- Check with the playbook owner before you go on when: the draft model level is not `mini` or `default`; the holdout set is empty; the baseline for publishing is unknown; a scenario is outstanding; or a mandatory expectation still fails.
- Keep the API key out of logs and shared output, and do not paste contract text into a playbook.

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
3. The quote supports the finding and contradicts the gold, which expects an uncapped liability. This is a gold problem. Report it to the gold file's owner with the contract, expectation and quote, and do not edit the gold. Leave the playbook alone for this failure.
4. Another contract, `supplier-d`, shows a different failure on the same clause. The `explanation` treats a cap that covers only indirect damages as a full liability cap. The quote supports the expected `flagged` verdict. The red flag wording does not say that a partial cap does not count, so this is playbook wording. Propose a clearer `whyProblematic` for the red flag, save it with a reason that names the failures it targets, then rerun tuning and holdout.
