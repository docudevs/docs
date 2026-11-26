---
title: CLI Reference
description: Complete CLI reference for DocuDevs including document processing, batch operations, configuration management, templates, cases, and OCR commands.
sidebar_position: 5
---
<!-- Generated from docs/reference/cli-reference.md. Do not edit by hand. -->

The DocuDevs CLI provides convenient command-line access to most of the platform. The CLI is included with the SDK installation and supports both high-level convenience commands and low-level API operations.

## Installation

The CLI is included when you install the DocuDevs SDK:

```bash
pip install docu-devs-api-client
```

## Authentication

The CLI supports multiple authentication methods:

### Environment Variables (Recommended)

```bash
# The variable the examples use
export API_KEY=your-api-key-here

# Also read by the CLI; it takes precedence over API_KEY when both are set
export DOCUDEVS_TOKEN=your-api-key-here

# Use commands without --token
docudevs process document.pdf
```

### Command Line Option

```bash
# Pass token with each command
docudevs --token=your-api-key-here process document.pdf
```

## High-Level Commands

These commands handle the complete workflow for common operations.

### `process`

Upload and process a document with AI extraction in one command.

```bash
docudevs process document.pdf [OPTIONS]
```

**Options:**

- `--prompt TEXT`: Extraction prompt describing what to extract
- `--prompt-file PATH`: Read extraction prompt from a text file
- `--schema TEXT`: JSON schema for structured extraction
- `--schema-file PATH`: Load JSON schema from file
- `--mime-type TEXT`: Override MIME type detection for the input file
- `--configuration TEXT`: Process using a named configuration
- `--ocr [DEFAULT|NONE|PREMIUM|AUTO|EXCEL]`: OCR processing type (default: DEFAULT)
- `--llm [DEFAULT|MINI|HIGH]`: LLM model to use (default: DEFAULT)
- `--barcodes`: Enable barcode and QR detection for this run
- `--extraction-mode [OCR|SIMPLE|STEPS|WORKBOOK]`: Force a specific extraction pipeline. `WORKBOOK` requires a nonblank `--prompt`, rejects `--schema`, and only accepts an `.xlsx` input file.
- `--describe-figures`: Request figure descriptions when supported
- `--timeout INTEGER`: Timeout in seconds (default: 60)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)
- `--tool JSON`: Attach a tool descriptor as JSON or `@path` (must include `type`; can be used multiple times)

**Examples:**

```bash
# Extract invoice data
docudevs process invoice.pdf --prompt="Extract invoice data including total, date, vendor"

# Use JSON schema for structured extraction
docudevs process contract.pdf --schema='{"type": "object", "properties": {"party1": {"type": "string"}}}'

# Use premium OCR and don't wait for result
docudevs process scan.pdf --ocr=PREMIUM --no-wait

# Read prompt and schema from files
docudevs process invoice.pdf --prompt-file instructions.txt --schema-file schema.json

# Process using a saved configuration and enable barcode detection
docudevs process receipt.pdf --configuration retail-config --barcodes
```

### `process-map-reduce`

Upload and process a document using map-reduce chunking.

```bash
docudevs process-map-reduce document.pdf [OPTIONS]
```

**Options:**

- `--prompt/--prompt-file`: Extraction instructions (same as `process`)
- `--schema/--schema-file`: JSON schema (same as `process`)
- `--mime-type TEXT`: Override MIME type detection
- `--ocr [DEFAULT|NONE|PREMIUM|AUTO|EXCEL]`: OCR processing type (default: DEFAULT)
- `--llm [DEFAULT|MINI|HIGH]`: LLM model to use (default: DEFAULT)
- `--barcodes`: Enable barcode/QR detection
- `--extraction-mode [OCR|SIMPLE|STEPS|WORKBOOK]`: Pipeline override. `WORKBOOK` is rejected for map-reduce processing.
- `--describe-figures`: Request figure descriptions when supported
- `--tool JSON`: Attach a tool descriptor as JSON or `@path` (must include `type`; can be used multiple times)
- `--pages-per-chunk INTEGER`: Pages per chunk (default: 1)
- `--overlap INTEGER`: Overlap between chunks (default: 0)
- `--dedup-key TEXT`: JSON path used to deduplicate rows across overlapping chunks
- `--split-type [page|markdown-header]`: Chunking mode (default: `page`)
- `--split-header-level INTEGER`: Header level (`1` or `2`) when `--split-type markdown-header`
- `--header-page-limit INTEGER`: Number of pages reserved for header extraction
- `--header-include-in-rows`: Include header pages in row processing
- `--header-row-prompt-augmentation TEXT`: Extra context injected into each chunk prompt
- `--header-schema/--header-schema-file`: Header-specific schema
- `--header-prompt TEXT`: Header-specific prompt
- `--header-page-index INTEGER`: Repeatable, explicit header page indices
- `--stop-when-empty`: Stop processing after empty chunks (use with `--empty-chunk-grace`)
- `--empty-chunk-grace INTEGER`: Number of empty chunks tolerated when `--stop-when-empty` is set
- `--timeout INTEGER`: Wait timeout in seconds (default: 60)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)

When using `--split-type markdown-header`, `--overlap` and `--dedup-key` are not supported.

**Example:**

```bash
docudevs process-map-reduce large-invoice.pdf \
  --prompt "Extract line items" \
  --schema-file schema.json \
  --pages-per-chunk 3 \
  --overlap 1 \
  --dedup-key "lineItems.sku" \
  --header-page-limit 1 \
  --header-schema '{"invoiceNumber":"string"}'

# Split by markdown subsection (##)
docudevs process-map-reduce handbook.pdf \
  --prompt "Extract policies by section" \
  --split-type markdown-header \
  --split-header-level 2
```

### `ocr-only`

Upload and process a document with OCR-only mode (no AI extraction).

```bash
docudevs ocr-only document.pdf [OPTIONS]
```

**Options:**

- `--ocr [DEFAULT|NONE|PREMIUM|AUTO|EXCEL]`: OCR processing type (default: DEFAULT)
- `--format [plain|markdown|jsonl]`: Output format (default: plain, or jsonl for EXCEL)
- `--timeout INTEGER`: Timeout in seconds (default: 60)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)

**Examples:**

```bash
# Basic OCR
docudevs ocr-only document.pdf

# OCR with markdown formatting
docudevs ocr-only document.pdf --format=markdown

# Premium OCR
docudevs ocr-only document.pdf --ocr=PREMIUM
```

### `wait`

Wait for a job to complete and return the result.

```bash
docudevs wait JOB_GUID [OPTIONS]
```

**Options:**

- `--timeout INTEGER`: Timeout in seconds (default: 60)

**Example:**

```bash
docudevs wait 550e8400-e29b-41d4-a716-446655440000
```

## Batch Processing

Upload and process multiple documents as one batch job.

### `batch process`

```bash
docudevs batch process FILE [FILE ...] [OPTIONS]
```

**Options:**

- `--prompt/--prompt-file`: Extraction instructions (same as `process`)
- `--schema/--schema-file`: JSON schema (same as `process`)
- `--mime-type TEXT`: Explicit MIME type for all documents
- `--ocr [DEFAULT|NONE|PREMIUM|AUTO|EXCEL]`: OCR processing type
- `--llm [DEFAULT|MINI|HIGH]`: LLM model to use
- `--barcodes`: Enable barcode and QR code detection
- `--extraction-mode [OCR|SIMPLE|STEPS|WORKBOOK]`: Extraction mode override. `WORKBOOK` is rejected for batch processing.
- `--describe-figures`: Request figure descriptions when supported
- `--max-concurrency INTEGER`: Maximum concurrent document processing within the batch
- `--format [json|csv|excel]`: Output format for results (implies `--wait`)
- `--output PATH`: Save results to this file (auto-generated from `--format` if omitted)
- `--configuration TEXT`: Named configuration for configuration-backed batch processing. Cannot be combined with `--prompt`, `--schema`, `--ocr`, `--llm`, `--barcodes`, `--extraction-mode`, or `--describe-figures`.
- `--lookup-file PATH`: Batch-scoped lookup file that overrides any configuration lookup file for this batch
- `--timeout INTEGER`: Timeout in seconds (default: 300)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)

**Examples:**

```bash
# Batch-process several invoices with a shared prompt
docudevs batch process invoice-1.pdf invoice-2.pdf invoice-3.pdf \
  --prompt "Extract invoice data" \
  --max-concurrency 5

# Batch-process using a saved configuration and export to Excel
docudevs batch process *.pdf --configuration invoice-config --format excel --output batch-results.xlsx
```

## Configuration Management

Manage named processing configurations for reusable workflows.

### `list-configurations`

List all saved configurations.

```bash
docudevs list-configurations
```

### `get-configuration`

Get details of a specific configuration.

```bash
docudevs get-configuration CONFIG_NAME
```

### `save-configuration`

Save a processing configuration from a JSON file.

```bash
docudevs save-configuration CONFIG_NAME config.json
```

**Example config.json:**

```json
{
  "prompt": "Extract invoice data",
  "schema": "{\"type\": \"object\"}",
  "ocr": "DEFAULT",
  "llm": "DEFAULT"
}
```

### `delete-configuration`

Delete a saved configuration.

```bash
docudevs delete-configuration CONFIG_NAME
```

### `configuration lookup-file upload`

Attach a lookup file to a named configuration. It applies to every job that uses the configuration until replaced or deleted.

```bash
docudevs configuration lookup-file upload CONFIG_NAME vendors.csv
```

### `configuration lookup-file delete`

Remove the lookup file attached to a configuration.

```bash
docudevs configuration lookup-file delete CONFIG_NAME
```

## Template Management

Work with document templates for form filling and extraction.

### `list-templates`

List all available templates.

```bash
docudevs list-templates
```

### `upload-template`

Upload a template document.

```bash
docudevs upload-template TEMPLATE_NAME template.pdf
```

- `--mime-type TEXT`: Override the detected MIME type

### `template-metadata`

Fetch metadata for a template.

```bash
docudevs template-metadata TEMPLATE_NAME
```

For newly uploaded PDF templates, metadata extraction is asynchronous. If the template was just uploaded, wait briefly and run the command again.

### `delete-template`

Delete a template.

```bash
docudevs delete-template TEMPLATE_NAME
```

### `fill`

Fill a template with data from a JSON file and optionally write the result to disk.

```bash
docudevs fill TEMPLATE_NAME data.json --output filled.pdf
```

**Example data.json:**

```json
{
  "fields": {
    "name": "John Doe",
    "address": "123 Main St",
    "date": "2024-01-15"
  }
}
```

## Case Management

Manage cases and documents stored within them.

### `cases list`

List all cases for the current organization.

```bash
docudevs cases list
```

### `cases create`

Create a new case.

**Options:**

- `--name TEXT`: Case name (required)
- `--description TEXT`: Optional case description

```bash
docudevs cases create --name "Quarterly Invoices" --description "Q4 2024 invoice processing"
```

### `cases get`

Retrieve details for a specific case.

```bash
docudevs cases get CASE_ID
```

### `cases update`

Update an existing case.

```bash
docudevs cases update CASE_ID --name "Quarterly Invoices (Updated)"
```

- `--name TEXT`: Case name (required)
- `--description TEXT`: Case description

### `cases delete`

Delete a case and all associated documents.

```bash
docudevs cases delete CASE_ID
```

### `cases upload-document`

Upload a document into a case.

**Options:**

- `--filename TEXT`: Custom filename for the uploaded document (defaults to the source file's name)
- `--mime-type TEXT`: Explicit MIME type for the document (defaults to detection from the filename)
- `--metadata TEXT`: Document metadata as a JSON object or comma-separated `key=value` pairs

```bash
docudevs cases upload-document CASE_ID invoice.pdf --metadata '{"department": "finance"}'
```

### `cases list-documents`

List documents stored in a case.

```bash
docudevs cases list-documents CASE_ID --page 0 --size 20
```

### `cases get-document`

Get metadata for a document stored in a case.

```bash
docudevs cases get-document CASE_ID DOCUMENT_ID
```

### `cases delete-document`

Remove a document from a case.

```bash
docudevs cases delete-document CASE_ID DOCUMENT_ID
```

### `cases get-summary`

Get the AI-generated summary for a document in a case.

```bash
docudevs cases get-summary CASE_ID DOCUMENT_ID
```

### `cases list-summaries`

List AI-generated summaries for all documents in a case.

```bash
docudevs cases list-summaries CASE_ID
```

## Knowledge Base Management

Manage knowledge bases derived from cases.

### `knowledge-base list`

List all knowledge bases.

```bash
docudevs knowledge-base list
```

### `knowledge-base add`

Promote a case to a knowledge base.

```bash
docudevs knowledge-base add CASE_ID
```

### `knowledge-base get`

Get a knowledge base by case ID.

```bash
docudevs knowledge-base get CASE_ID
```

### `knowledge-base remove`

Demote a case from knowledge base status.

```bash
docudevs knowledge-base remove CASE_ID
```

## Contract Analysis

Manage a knowledge base's contract-analysis profile, generations, test suite, trials, and immutable analysis runs. All commands are nested under `knowledge-base contract-analysis`. See [Contract-analysis playbooks](https://docs.docudevs.ai/docs/core/contract-analysis-playbooks) for building, testing, publishing and running a playbook.

### `knowledge-base contract-analysis get`

Get a contract-analysis profile draft or its published version.

```bash
docudevs knowledge-base contract-analysis get CASE_ID --view draft
```

- `--view [draft|published]`: Which version to fetch (default: `draft`)

### `knowledge-base contract-analysis generate`

Start the first build of clauses from the guideline documents of a playbook. A playbook that already has clauses is refused with status `400` (`REJECTED`); change such a playbook by exporting, editing and importing it.

```bash
docudevs knowledge-base contract-analysis generate CASE_ID --our-party "Example Supplier" --counterparty "Example Customer"
```

- `--our-party TEXT`: The side the playbook is written for (required)
- `--counterparty TEXT`: The other side of the agreements (required)

### `knowledge-base contract-analysis generation-compare`

Show the comparison for a build job. For a playbook in the clause format it is empty: the call answers `200` with `classification` `unchanged` and no `elements`. An unknown job ID returns `404`.

```bash
docudevs knowledge-base contract-analysis generation-compare CASE_ID GENERATION_JOB_GUID
```

### `knowledge-base contract-analysis generation-apply`

Apply per-element decisions from a comparison. For a playbook in the clause format there is nothing to apply and the draft is not changed: any decision returns `409` (code `generation_apply_conflict`), and an empty decisions object returns `422` with the problem `UNKNOWN_FIELD` at `criteria`.

```bash
docudevs knowledge-base contract-analysis generation-apply CASE_ID GENERATION_JOB_GUID --decisions decisions.json
```

- `--decisions PATH`: JSON object mapping each changed lineage ID to an explicit action and optional reason (required)

### `knowledge-base contract-analysis update`

Replace the draft profile at the observed revision.

```bash
docudevs knowledge-base contract-analysis update CASE_ID --draft-revision 3 --profile profile.json
```

- `--draft-revision INTEGER`: Observed draft revision (required)
- `--profile PATH` (aliases `--profile-file`, `--input`): Draft profile JSON file (required)
- `--reason TEXT`: Why the draft changed, recorded in the draft change log (at most 500 characters)

### `knowledge-base contract-analysis publish`

Publish a validated draft profile at the observed revision.

```bash
docudevs knowledge-base contract-analysis publish CASE_ID --draft-revision 3 --acknowledge-warning missing_termination_clause
```

- `--draft-revision INTEGER`: Observed draft revision (required)
- `--acknowledge-warning` (aliases `--acknowledge-warning-code`, `--warning-code`): Warning code to acknowledge (repeatable)
- `--trial-attestation-id TEXT`: Optional trial attestation ID backing the publish

### `knowledge-base contract-analysis suite-get`

Read the current immutable contract-analysis test suite.

```bash
docudevs knowledge-base contract-analysis suite-get CASE_ID
```

### `knowledge-base contract-analysis suite-update`

Create the next suite revision from a JSON array of test cases.

```bash
docudevs knowledge-base contract-analysis suite-update CASE_ID --revision 2 --cases cases.json
```

- `--revision INTEGER`: Observed suite revision (required)
- `--cases PATH`: JSON array of test cases (required)

### `knowledge-base contract-analysis readiness`

Read the current suite readiness and the blockers to publication.

```bash
docudevs knowledge-base contract-analysis readiness CASE_ID
```

### `knowledge-base contract-analysis package-prepare`

Capture an immutable test package without launching paid analysis. See [Prepare a test package](https://docs.docudevs.ai/docs/core/contract-analysis-playbooks#1-prepare-a-test-package).

```bash
docudevs knowledge-base contract-analysis package-prepare CASE_ID \
  --anchor-job-guid JOB_GUID \
  --idempotency-key package-1 \
  --as-of-date 2026-01-31
```

- `--anchor-job-guid TEXT`: Anchor job GUID (required)
- `--idempotency-key TEXT`: Idempotency key (required)
- `--package-members PATH`: JSON array of selected `{jobGuid, role}` documents
- `--expected-missing PATH`: JSON file of expected package documents that are unavailable
- `--excluded-documents PATH`: JSON file of documents excluded from the package
- `--as-of-date TEXT`: Contract context date in strict `YYYY-MM-DD` format

### `knowledge-base contract-analysis trial-estimate`

Show the current trial work estimate and the configured execution ceilings. Run it before `trial`: the launch takes the fingerprints this estimate returns. See [Estimate and run a scored trial](https://docs.docudevs.ai/docs/core/contract-analysis-playbooks#4-estimate-and-run-a-scored-trial).

```bash
docudevs knowledge-base contract-analysis trial-estimate CASE_ID \
  --anchor-job-guid JOB_GUID \
  --kind scored
```

- `--anchor-job-guid TEXT`: Anchor job GUID (required)
- `--suite-revision INTEGER`: Optional suite revision to estimate against
- `--kind [exploratory|scored]`: Trial kind (default: `exploratory`)
- `--suite-case-id TEXT`: Optional single suite case to estimate
- `--package-id TEXT`: Optional captured package ID

### `knowledge-base contract-analysis trial`

Launch an explicit draft-profile trial and print its trial/run identities.

```bash
docudevs knowledge-base contract-analysis trial CASE_ID \
  --anchor-job-guid JOB_GUID \
  --idempotency-key trial-1 \
  --kind scored \
  --estimate-fingerprint ESTIMATE_FINGERPRINT \
  --configuration-fingerprint CONFIGURATION_FINGERPRINT
```

- `--anchor-job-guid TEXT`: Anchor job GUID (required)
- `--idempotency-key TEXT`: Idempotency key (required)
- `--estimate-fingerprint TEXT`: Estimate fingerprint from `trial-estimate` (required)
- `--configuration-fingerprint TEXT`: Execution fingerprint from `trial-estimate` (required)
- `--suite-revision INTEGER`: Optional suite revision to trial against
- `--kind [exploratory|scored]`: Trial kind (default: `exploratory`)
- `--suite-case-id TEXT`: Optional single suite case to trial

### `knowledge-base contract-analysis trial-get`

Get exactly one trial by immutable trial ID.

```bash
docudevs knowledge-base contract-analysis trial-get CASE_ID TRIAL_ID
```

### `knowledge-base contract-analysis trial-attest`

Record expert sign-off for an exact immutable trial report.

```bash
docudevs knowledge-base contract-analysis trial-attest CASE_ID TRIAL_ID --report-id REPORT_ID
```

- `--report-id TEXT`: Report ID to attest (required)
- `--reason TEXT`: Optional reason

### `knowledge-base contract-analysis run-create`

Create a run and return its immutable run ID.

```bash
docudevs knowledge-base contract-analysis run-create KNOWLEDGE_BASE_ID \
  --anchor-job-guid JOB_GUID \
  --idempotency-key run-1
```

- `--anchor-job-guid TEXT`: Anchor job GUID (required)
- `--idempotency-key TEXT`: Idempotency key (required)
- `--mode [published|draft_trial]`: Run mode (default: `published`)
- `--draft-revision INTEGER`: Required when `--mode draft_trial`
- `--package-members PATH`: JSON array of selected `{jobGuid, role}` documents (maximum 32)
- `--expected-missing PATH`: JSON array of unavailable expected package documents
- `--excluded-documents PATH`: JSON array of explicitly excluded package documents

### `knowledge-base contract-analysis run-get`

Get status for exactly one immutable run ID.

```bash
docudevs knowledge-base contract-analysis run-get RUN_ID
```

### `knowledge-base contract-analysis run-result`

Get the result for exactly one immutable run ID.

```bash
docudevs knowledge-base contract-analysis run-result RUN_ID
```

### `knowledge-base contract-analysis run-reviews`

List the current human review dispositions for an immutable run.

```bash
docudevs knowledge-base contract-analysis run-reviews RUN_ID
```

### `knowledge-base contract-analysis review`

Append a human disposition to one clause finding of a captured result.

```bash
docudevs knowledge-base contract-analysis review RUN_ID CLAUSE_ID \
  --result-fingerprint sha256:... \
  --disposition accepted \
  --expected-review-revision 0 \
  --idempotency-key review-1
```

- `--result-fingerprint TEXT`: Result fingerprint being reviewed (required)
- `--disposition [accepted|corrected|dismissed|unresolved]`: Disposition (required)
- `--expected-review-revision INTEGER`: Observed review revision (required)
- `--idempotency-key TEXT`: Idempotency key (required)
- `--corrected-verdict [satisfactory|mixed|flagged|ai_unsure|not_found|does_not_apply]`: Required with `--disposition corrected`
- `--corrected-red-flag TEXT`: Corrected red flag as `id=triggered` or `id=not_triggered` (repeatable)
- `--reason TEXT`: Optional reason
- `--evidence PATH`: Optional JSON array of immutable evidence references

### `knowledge-base contract-analysis propose-test-case`

Propose an unapproved suite expectation from one clause finding.

```bash
docudevs knowledge-base contract-analysis propose-test-case CASE_ID \
  --run-id RUN_ID \
  --result-fingerprint sha256:... \
  --clause-id hours \
  --name "Hours threshold" \
  --expected-verdict satisfactory \
  --reason "Correction" \
  --expected-suite-revision 3 \
  --idempotency-key proposal-1
```

- `--run-id`, `--result-fingerprint`, `--clause-id`, `--name`, `--reason`, `--expected-suite-revision`, `--idempotency-key`: All required
- `--expected-verdict [satisfactory|mixed|flagged|ai_unsure|not_found|does_not_apply]`: Required
- `--expected-red-flag TEXT`: Optional red flag as `id=triggered` or `id=not_triggered` (repeatable)
- `--notes TEXT`: Optional notes

## Operations Management

Run post-processing operations such as error analysis and generative tasks.

### `operations submit`

Submit an operation by type.

```bash
docudevs operations submit JOB_GUID --type error-analysis --parameter quality=deep
```

- `--type TEXT`: Operation type to execute (required)
- `--llm-type [DEFAULT|MINI|HIGH]`: Optional LLM override
- `--parameter key=value`: Custom operation parameter (repeatable)
- `--wait/--no-wait`: Wait for the operation result (default: `--no-wait`)
- `--timeout INTEGER`: Wait timeout in seconds (default: 120)
- `--poll-interval FLOAT`: Polling interval in seconds (default: 2.0)

### Contract analysis operation

Run contract analysis against a published knowledge-base profile with `operations submit` and the `contract-analysis` type.

```bash
docudevs operations submit JOB_GUID \
  --type contract-analysis \
  --parameter knowledgeBaseId=KNOWLEDGE_BASE_ID \
  --wait
```

Use the options of `operations submit` above. The knowledge base is passed as the `knowledgeBaseId` parameter.

### `operations error-analysis`

Convenience command for error analysis.

```bash
docudevs operations error-analysis JOB_GUID --timeout 180
```

- `--llm-type [DEFAULT|MINI|HIGH]`: Optional LLM fallback
- `--parameter key=value`: Custom parameter (repeatable)
- `--wait/--no-wait`: Wait for completion (default: wait)
- `--timeout INTEGER`: Wait timeout in seconds (default: 120)
- `--poll-interval FLOAT`: Polling interval in seconds (default: 2.0)

### `operations generative-task`

Create a generative task from a completed job.

```bash
docudevs operations generative-task PARENT_JOB_GUID --prompt "Summarize the findings" --model DEFAULT
```

- `--prompt TEXT`: Prompt for the generative task (required)
- `--model TEXT`: Optional LLM model override
- `--temperature FLOAT` and `--max-tokens INTEGER` mirror API parameters
- `--wait/--no-wait`: Wait for task completion (default: wait); `--no-wait` returns immediately with the operation job GUID
- `--timeout INTEGER`: Wait timeout in seconds (default: 120)
- `--poll-interval FLOAT`: Polling interval in seconds (default: 2.0)

### `operations pdf-acroform`

Convert a completed PDF job into a generated fillable AcroForm PDF.

```bash
docudevs operations pdf-acroform PARENT_JOB_GUID \
  --ocr PREMIUM \
  --page 1 \
  --page 2 \
  --min-confidence 0.35 \
  --max-fields-per-page 300 \
  --timeout 1200 \
  --poll-interval 10 \
  --output medical-examination-form-fillable.pdf
```

- Parent job must refer to a PDF
- `--llm-type [DEFAULT|MINI|HIGH]`: LLM type for visual field detection (default: `DEFAULT`)
- `--ocr [PREMIUM|DEFAULT|AUTO]`: OCR mode used when page images are missing (default: `PREMIUM`, the safest choice when the parent job may not already have thumbnails)
- `--page INTEGER`: 1-based page number to inspect (repeatable)
- `--min-confidence FLOAT`: Minimum accepted field detection confidence
- `--max-fields-per-page INTEGER`: Maximum generated fields per page
- `--force-ocr/--no-force-ocr` regenerates page images before detection even if thumbnails already exist
- `--wait/--no-wait`: Wait for completion (default: wait)
- `--timeout INTEGER` / `--poll-interval FLOAT`: Wait timeout and polling interval (defaults: 180 / 5.0)
- `--output` requires the default wait mode because the command downloads the generated PDF after completion
- The JSON response includes `operationJobGuid` for later metadata or field-definition retrieval through the SDK or HTTP API

### `operations status`

List operations created for a job.

```bash
docudevs operations status JOB_GUID
```

### `operations result`

Fetch the result payload for a completed operation.

```bash
docudevs operations result JOB_GUID --type error-analysis
```

## Billing

The CLI has no `billing` command group and the Python SDK client has no billing helper methods (there is no `list_billing_prices`, `create_billing_checkout_session`, or `get_billing_balance`). Credit packs and balance are managed in the DocuDevs web app; see [Billing and credits](https://docs.docudevs.ai/docs/reference/billing-credits).

## LLM Provider Management

Manage LLM providers and key bindings.

### `llm providers`

List LLM providers.

```bash
docudevs llm providers
```

### `llm create`

Create an LLM provider.

:::warning
This command does not work with the current API. It sends `type`, `baseUrl` and `model`, and `POST /llm/providers` requires `apiUrl` and `modelType`, so the request is rejected. Create the provider with the cURL example on [Bring your own LLM](https://docs.docudevs.ai/docs/administration/bring-your-own-llm#create-a-provider).
:::

```bash
docudevs llm create --name "My OpenAI" --type OPENAI --api-key "sk-..."
```

### `llm get`

Get LLM provider by ID.

```bash
docudevs llm get PROVIDER_ID
```

### `llm update`

Update LLM provider. Only `--name` takes effect: the API ignores the other options this command sends. Use the cURL example on [Bring your own LLM](https://docs.docudevs.ai/docs/administration/bring-your-own-llm#update-or-delete-a-provider) to change other fields.

```bash
docudevs llm update PROVIDER_ID --name "New Name"
```

### `llm delete`

Delete (soft) an LLM provider.

```bash
docudevs llm delete PROVIDER_ID
```

### `llm keys`

List LLM key bindings.

```bash
docudevs llm keys
```

### `llm bind`

Bind (or clear) a logical LLM key to a provider.

```bash
docudevs llm bind KEY --provider-id PROVIDER_ID
```

## OCR Provider Management

Manage OCR providers and key bindings.

### `ocr providers`

List OCR providers.

```bash
docudevs ocr providers
```

### `ocr create`

Create an OCR provider.

:::warning
This command does not work with the current API. It sends `model`, and `POST /ocr/providers` requires `modelId`, so the request is rejected. Create the provider with the cURL example on [Bring your own OCR](https://docs.docudevs.ai/docs/administration/bring-your-own-ocr#create-a-provider).
:::

```bash
docudevs ocr create --name "My Azure OCR" --endpoint "https://example.cognitiveservices.azure.com" --api-key "..."
```

### `ocr get`

Get OCR provider by ID.

```bash
docudevs ocr get PROVIDER_ID
```

### `ocr update`

Update OCR provider. Only `--name` and `--endpoint` take effect: the API ignores `--model` and `--description`. Use the cURL example on [Bring your own OCR](https://docs.docudevs.ai/docs/administration/bring-your-own-ocr#update-or-delete-a-provider) to change other fields.

```bash
docudevs ocr update PROVIDER_ID --name "New Name"
```

### `ocr delete`

Delete (soft) an OCR provider.

```bash
docudevs ocr delete PROVIDER_ID
```

### `ocr keys`

List OCR key bindings.

```bash
docudevs ocr keys
```

### `ocr bind`

Bind (or clear) an OCR key to a provider.

```bash
docudevs ocr bind KEY --provider-id PROVIDER_ID
```

## Low-Level Commands

These commands provide direct access to individual API operations.

### `status`

Check the status of a processing job.

```bash
docudevs status JOB_GUID
```

### `result`

Get the result of a completed job.

```bash
docudevs result JOB_GUID
```

## Records

Manage the records dataset of a case: its configuration, single records, bulk loads, imports and search. See [Records datasets and field mapping](https://docs.docudevs.ai/docs/advanced/records-datasets) and [Importing records](https://docs.docudevs.ai/docs/advanced/records-import).

### `records dataset-set`

Configure the dataset from a JSON file with `fieldMapping` and the optional `embeddedText`, `examplePolicy` and `indexSettings`.

```bash
docudevs records dataset-set CASE_ID dataset.json --reindex
```

- `--reindex`: Confirm re-embedding of existing records

### `records dataset-get`

Show the dataset configuration.

```bash
docudevs records dataset-get CASE_ID
```

### `records upsert`

Upsert one record.

```bash
docudevs records upsert CASE_ID SKU-1001 --text title="Steel bolt" --metadata supplier=example
```

- `--text TEXT`: Text field as `name=value` (repeatable, at least one required)
- `--label TEXT`: Category code
- `--label-source [import|review]`: Origin of the label
- `--metadata TEXT`: Metadata as `name=value` (repeatable)

### `records get`

Show one record.

```bash
docudevs records get CASE_ID SKU-1001
```

### `records delete`

Delete one record.

```bash
docudevs records delete CASE_ID SKU-1001
```

### `records list`

List records ordered by key.

```bash
docudevs records list CASE_ID --subtree FASTENERS --all
```

- `--label TEXT`: Exact category code
- `--subtree TEXT`: Category code whose subtree to list
- `--label-source [import|review]`: Filter by label origin
- `--attribute TEXT`: Metadata filter as `name=value` (repeatable)
- `--limit INTEGER`: Page size (1-200)
- `--continuation TEXT`: Continuation token of the previous page
- `--all`: Follow continuation tokens and print every record

### `records stats`

Show per-label record counts.

```bash
docudevs records stats CASE_ID --size 20
```

- `--page INTEGER`: Zero-based category page
- `--size INTEGER`: Categories per page (default 50)
- `--q TEXT`: Case-insensitive category-code substring

### `records bulk`

Upsert records from an ndjson or csv file. The command sends a batch when the file is small and a file import otherwise.

```bash
docudevs records bulk CASE_ID records.ndjson --wait
```

- `--format [ndjson|csv]`: Input format (default: from the file name)
- `--mapping-file PATH`: JSON field mapping overriding the dataset mapping
- `--wait/--no-wait`: Wait for the import to finish (default: `--no-wait`)

### `records import-url`

Import records from an HTTPS URL fetched by DocuDevs.

```bash
docudevs records import-url CASE_ID https://example.com/records.csv.gz --format csv --gzip --wait
```

- `--format [ndjson|csv]`: File format (required)
- `--mode [upsert|sync]`: Import mode (default: `upsert`)
- `--gzip`: The file at the URL is gzip-compressed
- `--mapping-file PATH`: JSON field mapping overriding the dataset mapping
- `--wait/--no-wait`: Wait for the import to finish (default: `--no-wait`)

### `records sync`

Synchronise the dataset with a full snapshot. Without `--confirm` the command runs a dry run; apply it by passing the import ID of the completed dry run.

```bash
docudevs records sync CASE_ID snapshot.ndjson
docudevs records sync CASE_ID snapshot.ndjson --confirm IMPORT_ID
```

- `--confirm INTEGER`: Apply for real; the import ID of the completed dry run
- `--format [ndjson|csv]`: Input format (default: from the file name)
- `--mapping-file PATH`: JSON field mapping overriding the dataset mapping
- `--max-delete-fraction FLOAT`: Largest share of existing records a sync may delete
- `--confirm-large-delete`: Allow a sync that exceeds the delete limit
- `--reviewed-records [keep|delete]`: What to do with reviewed records absent from the snapshot
- `--reviewed-label-policy [reviewWins|importWins]`: Which label wins for a reviewed record
- `--wait/--no-wait`: Wait and print the report (default: wait)

### `records imports`

List a case's imports, newest first.

```bash
docudevs records imports CASE_ID --limit 20
```

- `--limit INTEGER`: Page size, 1 to 200
- `--continuation TEXT`: Token of the previous page

### `records import-status`

Show an import's status, counters and sync summary.

```bash
docudevs records import-status CASE_ID IMPORT_ID --wait
```

- `--wait/--no-wait`: Wait for the import to finish (default: `--no-wait`)

### `records import-retry`

Requeue a failed or stuck import.

```bash
docudevs records import-retry CASE_ID IMPORT_ID
```

### `records search`

Search the records or documents of a case. See [Case search](https://docs.docudevs.ai/docs/advanced/case-search).

```bash
docudevs records search CASE_ID "steel bolt" --kind records --top-k 5
```

- `--kind [records|documents]`: What to search (default: `records`)
- `--top-k INTEGER`: Number of results
- `--exhaustive/--no-exhaustive`: Search exhaustively or not
- `--oversampling FLOAT`: Oversampling factor
- `--hybrid/--no-hybrid`: Combine keyword and vector search or not
- `--semantic-rerank/--no-semantic-rerank`: Apply semantic reranking or not
- `--subtree TEXT`: Restrict records to a category subtree
- `--label-source [import|review]`: Restrict records by label origin
- `--attribute TEXT`: Metadata filter as `name=value` (repeatable)
- `--document-id TEXT`: Restrict documents to these job IDs (repeatable)

## Category Trees

Manage the category trees of a records case. See [Category trees and activation](https://docs.docudevs.ai/docs/advanced/category-trees).

### `trees upload`

Upload a JSON tree, either a node list or `{name, nodes}`, as a draft version.

```bash
docudevs trees upload CASE_ID tree.json --name "Fasteners v1"
```

- `--name TEXT`: Version name

### `trees list`

List tree versions.

```bash
docudevs trees list CASE_ID
```

### `trees node`

Show one category with its path, children and siblings.

```bash
docudevs trees node CASE_ID VERSION_ID FASTENERS
```

### `trees activate`

Activate a tree version.

```bash
docudevs trees activate CASE_ID VERSION_ID --successor-file successors.json --wait
```

- `--successor-file PATH`: JSON object mapping retired or missing codes to active codes
- `--allow-missing`: Activate although record labels name unknown codes
- `--wait/--no-wait`: Wait for the activation job (default: `--no-wait`)

### `trees activation-report`

Show the report of a tree activation. Find the import ID with `records imports`.

```bash
docudevs trees activation-report CASE_ID VERSION_ID IMPORT_ID
```

## Case Memo

Manage the classification memo of a records case. See [Case memo](https://docs.docudevs.ai/docs/advanced/case-memo).

### `memo add`

Add an instruction or a feedback entry. An instruction is proposed until it is approved.

```bash
docudevs memo add CASE_ID instruction "Treat bolts with a hex head as fasteners" --scope subtree --code FASTENERS
```

- `TYPE`: `instruction` or `feedback`
- `--scope [global|subtree|record|document]`: Scope of the entry (default: `global`)
- `--code TEXT`: Category code of a subtree scope
- `--key TEXT`: Record key of a record scope
- `--document-id TEXT`: Document ID of a document scope
- `--links-file PATH`: JSON file with `classification_results` and `record_keys`

### `memo list`

List memo entries.

```bash
docudevs memo list CASE_ID --status proposed
```

- `--type [instruction|feedback|correction]`: Filter by entry type
- `--scope [global|subtree|record|document]`: Filter by scope
- `--status [proposed|active|retired]`: Filter by status
- `--version INTEGER`: Read the snapshot at this memo version

### `memo approve`

Approve a proposed instruction.

```bash
docudevs memo approve CASE_ID ENTRY_ID
```

### `memo retire`

Retire a memo entry.

```bash
docudevs memo retire CASE_ID ENTRY_ID
```

### `memo revisions`

List every revision of a memo entry.

```bash
docudevs memo revisions CASE_ID ENTRY_ID
```

## Classification

Classify rows or records against a category tree or a knowledge base. See [Classification and matching](https://docs.docudevs.ai/docs/advanced/classification).

### `classify start`

Start a classify job from exactly one of `--rows-file`, `--keys-file` or `--file`.

```bash
docudevs classify start CASE_ID --keys-file keys.txt --max-total-tokens 100000 --wait
```

- `--rows-file PATH`: JSON list of inline rows `{key, text, metadata}`
- `--keys-file PATH`: Record keys, one per line
- `--file PATH`: ndjson, jsonl or csv file (optionally `.gz`) to upload
- `--format [ndjson|csv]`: File format when the name does not say
- `--config-file PATH`: JSON classify config
- `--max-total-tokens INTEGER`: Token budget; required for uploaded files
- `--wait/--no-wait`: Wait for the job to finish (default: `--no-wait`)

### `classify status`

Show the status, counts, usage and pinned versions of a classify job.

```bash
docudevs classify status CASE_ID JOB_ID --wait
```

- `--wait/--no-wait`: Wait for the job to finish (default: `--no-wait`)

### `classify list`

List a case's classify jobs, newest first.

```bash
docudevs classify list CASE_ID --limit 20
```

- `--limit INTEGER`: Page size, 1 to 200
- `--continuation TEXT`: Token of the previous page

### `classify cancel`

Cancel a classify job.

```bash
docudevs classify cancel CASE_ID JOB_ID
```

### `classify results`

Show classify result rows.

```bash
docudevs classify results CASE_ID JOB_ID --review-required --all
```

- `--flag TEXT`: Rows with this flag
- `--review-required/--no-review-required`: Rows that need (or do not need) review
- `--level INTEGER`: Tree level; with `--code`, matches the chosen code at that level
- `--code TEXT`: Chosen code
- `--decision [accept|correct|feedback|none]`: Review state
- `--routing TEXT`: Comma-separated flags that route a row to review
- `--limit INTEGER`: Page size, 1 to 1000
- `--continuation TEXT`: Token of the previous page
- `--all`: Follow every page and print one list

### `classify csv`

Export classify results as CSV. It takes the same filters as `classify results`.

```bash
docudevs classify csv CASE_ID JOB_ID --decision none --output results.csv
```

- `--flag`, `--review-required/--no-review-required`, `--level`, `--code`, `--decision`, `--routing`: As in `classify results`
- `--output FILE` (alias `-o`): Write the CSV to this file instead of stdout

### `classify review`

Accept, correct or comment on one result row.

```bash
docudevs classify review CASE_ID JOB_ID ROW_ID correct --code FASTENERS --reason "Wrong category"
```

- `DECISION`: `accept`, `correct` or `feedback`
- `--code TEXT`: The right code (for `correct`)
- `--reason TEXT`: Why the model was wrong (for `correct`)
- `--text TEXT`: The comment (for `feedback`)

## Case Agent

See [Case agent](https://docs.docudevs.ai/docs/advanced/case-agent), [Agent actions](https://docs.docudevs.ai/docs/advanced/agent-actions) and [Agent runs](https://docs.docudevs.ai/docs/advanced/agent-runs).

### `agent actions`

List the pending actions of a case, newest first.

```bash
docudevs agent actions CASE_ID --status pending --limit 20
```

- `--status TEXT`: Filter by status, such as `pending`
- `--limit INTEGER`: Page size
- `--before INTEGER`: Paging cursor from the previous page

### `agent confirm`

Confirm a pending action as the signed-in person. The stored request is replayed.

```bash
docudevs agent confirm ACTION_ID
```

### `agent reject`

Reject a pending action, optionally with a reason.

```bash
docudevs agent reject ACTION_ID --reason "Not needed"
```

- `--reason TEXT`: Why the action is rejected

### `agent run start`

Start a headless agent run and print its run ID. `PLAYBOOK` is `general` (needs `--goal`) or `investigate-flagged` (needs `job_id` in `--params-file`).

```bash
docudevs agent run start CASE_ID investigate-flagged --params-file params.json --budget-tokens 60000 --wait
```

- `--goal TEXT`: What the run should do; required by the `general` playbook
- `--params-file PATH`: JSON file with `max_rows`, `flags` and `job_id`
- `--budget-tokens INTEGER`: Tokens the run may use in total; authorised when the run starts
- `--wait/--no-wait`: Wait for the run to finish (default: `--no-wait`)

### `agent run status`

Show the status, usage, pending actions and report of a run.

```bash
docudevs agent run status CASE_ID RUN_ID --wait
```

- `--wait/--no-wait`: Wait for the run to finish (default: `--no-wait`)

### `agent run cancel`

Cancel a run at its next turn boundary.

```bash
docudevs agent run cancel CASE_ID RUN_ID
```

## Holdout Evaluation

Holdout evaluations score classification variants on a case. See [Holdout evaluation and memo A/B](https://docs.docudevs.ai/docs/advanced/case-evals). For contract-analysis playbooks use the commands under [Playbooks and Playbook Evaluation](#playbooks-and-playbook-evaluation).

### `eval holdout`

Start a holdout evaluation. `--variants-file` is a JSON list of `{"name", "memo_include"?, "config"?}`; the first entry is the baseline.

```bash
docudevs eval holdout CASE_ID --variants-file variants.json --sample-size 200 --seed 7 --group-field base_product_id --wait
```

- `--variants-file PATH`: JSON list of variants (required)
- `--sample-size INTEGER`: Rows in the scored sample, 10 to 5000 (default 200)
- `--seed INTEGER`: Seed of the grouped split
- `--group-field TEXT`: Record metadata key that groups variants of one product
- `--grouping-mode [nearDuplicate|none]`: `nearDuplicate` (default) leaves out near-duplicates of the sample; `none` is ungrouped and its accuracy is optimistic
- `--grouping-threshold FLOAT`: Cosine similarity at or above which a record is a near-duplicate (0 to 1, default 0.98)
- `--config-file PATH`: JSON classify settings shared by the variants
- `--wait/--no-wait`: Wait for the evaluation to finish (default: `--no-wait`)

### `eval compare`

Score two completed evaluations against each other without classifying again. Choose variants with `--baseline-variant` and `--candidate-variant`.

```bash
docudevs eval compare CASE_ID BASELINE_EVAL_ID CANDIDATE_EVAL_ID --baseline-variant baseline --candidate-variant with-rule
```

- `--baseline-variant TEXT`: Variant of the baseline evaluation (default: its first)
- `--candidate-variant TEXT`: Variant of the candidate evaluation (default: its first)

## Playbooks and Playbook Evaluation

Test a contract-analysis playbook against gold answers and edit its draft. See [Playbook evaluation](https://docs.docudevs.ai/docs/core/playbook-evaluation) for the whole procedure.

### `eval status`

Show an evaluation's status. A holdout evaluation is read by its number; a contract-analysis evaluation by its ID.

```bash
docudevs eval status CASE_ID EVALUATION_ID --wait
```

- `--wait/--no-wait`: Wait for the evaluation to finish (default: `--no-wait`)

### `eval list`

List a case's contract-analysis evaluations, newest first, with their status and trial counts.

```bash
docudevs eval list CASE_ID
```

### `eval import`

Import a gold table as approved contract-analysis test cases.

```bash
docudevs eval import CASE_ID --gold gold.xlsx --mapping mapping.yaml --dry-run
```

- `--gold PATH`: Gold table (CSV, XLSX or JSON rows) (required)
- `--mapping PATH`: YAML or JSON mapping from the table to clauses and facts (required)
- `--dry-run`: Validate the table and report what would be written, without writing

### `eval split`

Assign test cases to the tuning and holdout sets as a new suite revision.

```bash
docudevs eval split CASE_ID --holdout 0.3 --seed 1
```

- `--holdout FLOAT`: Share of each scenario held back for the holdout set (0 to 1, default: 0.3)
- `--seed INTEGER`: Seed for the held-back selection (default: 1)

### `eval run`

Start one scored trial per test case and wait for the results.

```bash
docudevs eval run CASE_ID --set tuning --repeats 2
```

- `--set [tuning|holdout|all]`: Which test cases to run (default: `all`)
- `--limit INTEGER`: Run only the first N cases of the set, in suite order
- `--llm-level [mini|default]`: Save this model level to the draft before running; omit it to keep the draft's level
- `--repeats INTEGER`: Run each case this many times (1 to 3) to find unstable expectations (default: 1)
- `--no-wait`: Print the evaluation ID and return without waiting for the trials

### `eval report`

Show an evaluation report as Markdown or JSON.

```bash
docudevs eval report CASE_ID EVALUATION_ID --against published --format json
```

- `--against TEXT`: Compare with another evaluation of the same case, or `published` for the newest settled evaluation of the published draft revision that covers every case of this evaluation, preferring the same suite revision
- `--format [json|md]`: Output format (default: `md`)

### `playbook export`

Export the playbook of a case as a document.

```bash
docudevs playbook export CASE_ID --view draft > playbook.json
```

- `--view [draft|published]`: Which version to export (default: `draft`)

### `playbook import`

Import a playbook document as the draft.

```bash
docudevs playbook import CASE_ID playbook.json --reason "Clarified the liability clause"
```

- `--draft-revision INTEGER`: Draft revision to replace (default: the current draft)
- `--reason TEXT`: Why the draft changed, recorded in the draft change log (at most 500 characters)

## Global Options

These options are available for all commands:

- `--api-url TEXT`: API endpoint URL (default `https://api.docudevs.ai`)
- `--token TEXT`: Authentication token (or use environment variables)
- `--help`: Show help message and exit

## Output Format

Most CLI commands return JSON-formatted output that can be parsed by other tools. Commands that produce binary content (for example `fill`) should be used with `--output` to write results to disk.

```bash
# Save result to file
docudevs process invoice.pdf > result.json

# Parse with jq
docudevs list-configurations | jq '.[] | .name'

# Check if command succeeded
if docudevs process document.pdf; then
    echo "Processing succeeded"
fi
```

## Error Handling

The CLI returns appropriate exit codes:

- `0`: Success
- `1`: Error (authentication, processing, etc.)

Error messages are written to stderr:

```bash
# Redirect errors to file
docudevs process document.pdf 2> errors.log

# Suppress errors
docudevs process document.pdf 2>/dev/null
```
