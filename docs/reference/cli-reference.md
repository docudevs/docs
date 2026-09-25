---
title: CLI Reference
description: Complete CLI reference for DocuDevs including document processing, batch operations, configuration management, templates, cases, and OCR commands.
sidebar_position: 5
---

The DocuDevs CLI provides convenient command-line access to all platform functionality. The CLI is included with the SDK installation and supports both high-level convenience commands and low-level API operations.

## Installation

The CLI is included when you install the DocuDevs SDK:

```bash
pip install docu-devs-api-client
```

## Authentication

The CLI supports multiple authentication methods:

### Environment Variables (Recommended)

```bash
# Primary environment variable
export DOCUDEVS_TOKEN=your-api-key-here

# Legacy support (also works)
export API_KEY=your-api-key-here

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

Manage a knowledge base's contract-analysis profile, generations, test suite, trials, and immutable analysis runs. All commands are nested under `knowledge-base contract-analysis`. See [Contract Analysis](/docs/core/contract-analysis) for the profile/draft/publish lifecycle and the run/review model.

### `knowledge-base contract-analysis get`

Get a contract-analysis profile draft or its published version.

```bash
docudevs knowledge-base contract-analysis get CASE_ID --view draft
```

- `--view [draft|published]`: Which version to fetch (default: `draft`)

### `knowledge-base contract-analysis generate`

Start profile compilation from the eligible knowledge-base documents.

```bash
docudevs knowledge-base contract-analysis generate CASE_ID
```

### `knowledge-base contract-analysis generation-compare`

Show the previous, current, and proposed values for a regeneration.

```bash
docudevs knowledge-base contract-analysis generation-compare CASE_ID GENERATION_JOB_GUID
```

### `knowledge-base contract-analysis generation-apply`

Apply explicit per-element decisions against the latest comparison fingerprint.

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

### `knowledge-base contract-analysis trial`

Launch an explicit draft-profile trial and print its trial/run identities.

```bash
docudevs knowledge-base contract-analysis trial CASE_ID \
  --anchor-job-guid JOB_GUID \
  --idempotency-key trial-1 \
  --kind scored
```

- `--anchor-job-guid TEXT`: Anchor job GUID (required)
- `--idempotency-key TEXT`: Idempotency key (required)
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
- `--package-members PATH`: JSON array of selected package documents (maximum 32)
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

Append a human disposition to one captured result finding.

```bash
docudevs knowledge-base contract-analysis review RUN_ID CRITERION_ID TARGET_KEY \
  --result-fingerprint sha256:... \
  --disposition accepted \
  --expected-review-revision 0 \
  --idempotency-key review-1
```

- `--result-fingerprint TEXT`: Result fingerprint being reviewed (required)
- `--disposition [accepted|corrected|dismissed|unresolved]`: Disposition (required)
- `--expected-review-revision INTEGER`: Observed review revision (required)
- `--idempotency-key TEXT`: Idempotency key (required)
- `--corrected-assessment TEXT`: Optional corrected assessment
- `--reason TEXT`: Optional reason
- `--evidence PATH`: Optional JSON array of immutable evidence references

### `knowledge-base contract-analysis propose-test-case`

Propose an unapproved suite expectation from one immutable finding.

```bash
docudevs knowledge-base contract-analysis propose-test-case CASE_ID \
  --run-id RUN_ID \
  --result-fingerprint sha256:... \
  --criterion-id hours \
  --target-key item:clause-1 \
  --name "Hours threshold" \
  --expected-assessment meets_requirement \
  --reason "Correction" \
  --expected-suite-revision 3 \
  --idempotency-key proposal-1
```

- `--run-id`, `--result-fingerprint`, `--criterion-id`, `--target-key`, `--name`, `--reason`, `--expected-suite-revision`, `--idempotency-key`: All required
- `--expected-assessment [meets_requirement|deviates_from_requirement|not_found|needs_review|not_applicable]`: Required
- `--scenario-tag TEXT`: Optional scenario tag (repeatable)

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

### `operations contract-analysis`

Run contract analysis against a published knowledge-base profile.

```bash
docudevs operations contract-analysis JOB_GUID --knowledge-base-id KNOWLEDGE_BASE_ID
```

- `--knowledge-base-id INTEGER` (alias `--knowledge-base`): Knowledge base ID (required)
- `--wait/--no-wait`: Wait for the result (default: wait)
- `--timeout FLOAT`: Wait timeout in seconds (default: 120)
- `--poll-interval FLOAT`: Polling interval in seconds (default: 2.0)

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

The CLI has no `billing` command group and the Python SDK client has no billing helper methods (there is no `list_billing_prices`, `create_billing_checkout_session`, or `get_billing_balance`). Token packs and balance are managed in the DocuDevs web app; see [Billing Tokens](/docs/reference/billing-tokens).

## LLM Provider Management

Manage LLM providers and key bindings.

:::warning
`llm create` and `llm update` call SDK convenience wrappers that currently send outdated field names and do not match the current provider contract. Use the generated API functions (see [Bring Your Own LLM](/docs/administration/bring-your-own-llm)) or the REST API directly instead of these two commands.
:::

### `llm providers`

List LLM providers.

```bash
docudevs llm providers
```

### `llm create`

Create an LLM provider.

```bash
docudevs llm create --name "My OpenAI" --type OPENAI --api-key "sk-..."
```

### `llm get`

Get LLM provider by ID.

```bash
docudevs llm get PROVIDER_ID
```

### `llm update`

Update LLM provider.

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

:::warning
`ocr create` and `ocr update` call SDK convenience wrappers that currently send outdated field names and do not match the current provider contract. Use the generated API functions (see [Bring Your Own OCR](/docs/administration/bring-your-own-ocr)) or the REST API directly instead of these two commands.
:::

### `ocr providers`

List OCR providers.

```bash
docudevs ocr providers
```

### `ocr create`

Create an OCR provider.

```bash
docudevs ocr create --name "My Azure OCR" --endpoint "https://..." --api-key "..."
```

### `ocr get`

Get OCR provider by ID.

```bash
docudevs ocr get PROVIDER_ID
```

### `ocr update`

Update OCR provider.

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
