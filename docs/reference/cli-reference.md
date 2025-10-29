---
title: CLI Reference
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
- `--configuration TEXT`: Process using a named configuration
- `--ocr [DEFAULT|NONE|PREMIUM|AUTO|EXCEL]`: OCR processing type (default: DEFAULT)
- `--llm [DEFAULT|MINI|HIGH]`: LLM model to use (default: DEFAULT)
- `--barcodes`: Enable barcode and QR detection for this run
- `--extraction-mode [OCR|SIMPLE|STEPS]`: Force a specific extraction pipeline
- `--describe-figures`: Request figure descriptions when supported
- `--timeout INTEGER`: Timeout in seconds (default: 60)
- `--wait/--no-wait`: Wait for processing to complete (default: wait)

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

### `ocr-only`

Upload and process a document with OCR-only mode (no AI extraction).

```bash
docudevs ocr-only document.pdf [OPTIONS]
```

**Options:**

- `--ocr [DEFAULT|NONE|PREMIUM|AUTO]`: OCR processing type (default: DEFAULT)
- `--format [plain|markdown]`: Output format (default: plain)
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

- `--api-url TEXT`: API endpoint URL (default https://api.docudevs.ai)
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

## Tips and Best Practices

### Use Environment Variables

Set up environment variables in your shell profile for convenience:

```bash
# Add to ~/.bashrc or ~/.zshrc
export DOCUDEVS_TOKEN=your-api-key-here
export DOCUDEVS_API_URL=https://api.docudevs.ai  # if using custom endpoint
```

### Batch Processing

Process multiple files with shell scripting:

```bash
#!/bin/bash
for file in *.pdf; do
    echo "Processing $file..."
    docudevs process "$file" --prompt="Extract key data" > "${file%.pdf}.json"
done
```

### Configuration Management

Save commonly used settings as configurations:

```bash
# Save invoice processing configuration
echo '{"prompt": "Extract invoice data", "ocr": "PREMIUM"}' > invoice-config.json
docudevs save-configuration invoice-processing invoice-config.json

# Use the configuration
docudevs process invoice.pdf --configuration invoice-processing
```

### Error Recovery

Handle temporary failures with retry logic:

```bash
#!/bin/bash
retry_count=0
max_retries=3

while [ $retry_count -lt $max_retries ]; do
    if docudevs process document.pdf; then
        echo "Success!"
        break
    else
        retry_count=$((retry_count + 1))
        echo "Attempt $retry_count failed, retrying..."
        sleep 5
    fi
done
```

## Troubleshooting

### Common Issues

**Authentication Error:**

```
Error: No authentication token provided
```

- Set `DOCUDEVS_TOKEN` environment variable or use `--token`

**Network/Connection Error:**

```
Error: Connection timeout
```

- Check internet connection and firewall settings
- Verify API endpoint with `--api-url`

**File Not Found:**

```
Error: Path "document.pdf" does not exist
```

- Verify file path is correct and file exists
- Use absolute paths if needed

### Debug Mode

For debugging, you can inspect the raw API responses:

```bash
# Use --no-wait to get the job GUID, then check status manually
docudevs process document.pdf --no-wait
# Copy the GUID from output
docudevs status YOUR_GUID_HERE
docudevs result YOUR_GUID_HERE
```

## See Also

- [SDK Methods Reference](/docs/reference/sdk-methods) - Python SDK documentation
- [API Documentation](/docs/openapi) - REST API reference
- [Error Codes](/docs/reference/error-codes) - Common error codes and solutions
