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
- `--schema TEXT`: JSON schema for structured extraction
- `--ocr [DEFAULT|NONE|PREMIUM|AUTO]`: OCR processing type (default: DEFAULT)
- `--llm [DEFAULT|MINI|HIGH]`: LLM model to use (default: DEFAULT)
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

### `fill`

Fill a template with data from a JSON file.

```bash
docudevs fill TEMPLATE_NAME data.json
```

**Example data.json:**

```json
{
  "name": "John Doe",
  "address": "123 Main St",
  "date": "2024-01-15"
}
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

All CLI commands return JSON-formatted output that can be parsed by other tools:

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
docudevs process-with-config invoice.pdf invoice-processing
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
