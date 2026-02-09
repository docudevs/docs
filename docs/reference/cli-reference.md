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
- `--extraction-mode [OCR|SIMPLE|STEPS]`: Force a specific extraction pipeline
- `--describe-figures`: Request figure descriptions when supported
- `--timeout INTEGER`: Timeout in seconds (default: 60)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)
- `--tool JSON`: Attach a tool descriptor (can be used multiple times)

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
- `--extraction-mode [OCR|SIMPLE|STEPS]`: Pipeline override
- `--describe-figures`: Request figure descriptions when supported
- `--pages-per-chunk INTEGER`: Pages per chunk (default: 1)
- `--overlap INTEGER`: Overlap between chunks (default: 0)
- `--dedup-key TEXT`: JSON path used to deduplicate rows across overlapping chunks
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
  "name": "John Doe",
  "address": "123 Main St",
  "date": "2024-01-15"
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

```bash
docudevs cases upload-document CASE_ID invoice.pdf
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

## Operations Management

Run post-processing operations such as error analysis and generative tasks.

### `operations submit`

Submit an operation by type.

```bash
docudevs operations submit JOB_GUID --type error-analysis --parameter quality=deep
```

- `--llm-type` optional LLM override (`DEFAULT`, `MINI`, `HIGH`)
- `--parameter` key/value pairs passed to the operation
- `--wait` to block until the operation completes

### `operations error-analysis`

Convenience command for error analysis.

```bash
docudevs operations error-analysis JOB_GUID --timeout 180
```

### `operations generative-task`

Create a generative task from a completed job.

```bash
docudevs operations generative-task PARENT_JOB_GUID --prompt "Summarize the findings" --model DEFAULT
```

- `--no-wait` returns immediately with the operation job GUID
- `--temperature` and `--max-tokens` mirror API parameters

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

Manage token packs and balances.

### `billing prices`

List available token pack prices.

```bash
docudevs billing prices
```

### `billing checkout`

Create a checkout session for a token pack.

```bash
docudevs billing checkout PRICE_ID
```

### `billing balance`

Fetch the current token balance.

```bash
docudevs billing balance
```

## LLM Provider Management

Manage LLM providers and key bindings.

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

- `--api-url TEXT`: API endpoint URL (default <https://api.docudevs.ai>)
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
