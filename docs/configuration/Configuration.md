---
title: Named Configurations
description: Save and reuse processing settings as named configurations for consistent document extraction across multiple jobs.
sidebar_position: 1
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Named Configurations

Save and reuse processing settings, prompts, and schemas across multiple document processing requests.

## Overview

Named configurations allow you to define a set of processing parameters once and reuse them by name. This ensures consistency across your application and simplifies your API calls.

**Key Benefits:**

- **Consistency**: Ensure all documents of a certain type are processed with the exact same settings.
- **Schema Management**: Centralize your JSON schemas to guarantee consistent output formats.
- **Simplicity**: Pass a single configuration name instead of a complex JSON object with every request.

## Managing Configurations

### Saving a Configuration

To create or update a configuration, you define the processing parameters (OCR, LLM, Prompt, Schema) and save them under a unique name.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCommand
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def save_invoice_config():
    # Define the configuration using UploadCommand
    config = UploadCommand(
        prompt="Extract invoice data including vendor details, line items, and totals.",
        schema={
            "type": "object",
            "properties": {
                "vendor_name": {"type": "string"},
                "invoice_number": {"type": "string"},
                "total_amount": {"type": "number"},
                "date": {"type": "string", "format": "date"}
            },
            "required": ["vendor_name", "total_amount"]
        },
        ocr="PREMIUM",
        llm="HIGH",
        extraction_mode="STEPS",
        barcodes=True
    )

    # Save the configuration with the name "invoice-config"
    result = await client.save_configuration("invoice-config", config)
    print(f"Configuration saved: {result}")

# asyncio.run(save_invoice_config())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Save a configuration from a JSON file
docudevs save-configuration invoice-config config.json

# Content of config.json:
# {
#   "prompt": "Extract invoice data...",
#   "schema": { ... },
#   "ocr": "PREMIUM",
#   "llm": "HIGH"
# }
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/configuration/invoice-config \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract invoice data including vendor details, line items, and totals.",
    "schema": {
      "type": "object",
      "properties": {
        "vendor_name": {"type": "string"},
        "invoice_number": {"type": "string"},
        "total_amount": {"type": "number"}
      }
    },
    "ocr": "PREMIUM",
    "llm": "HIGH",
    "extractionMode": "STEPS",
    "barcodes": true
  }'
```

  </TabItem>
</Tabs>

### Listing Configurations

Retrieve a list of all saved configurations in your organization.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
configs = await client.list_configurations()
for config in configs:
    print(f"Name: {config.name}, Created: {config.created_at}")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs list-configurations
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X GET https://api.docudevs.ai/configuration \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

### Retrieving a Configuration

Get the details of a specific configuration to inspect its settings.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
config = await client.get_configuration("invoice-config")
print(f"Prompt: {config.prompt}")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs get-configuration invoice-config
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X GET https://api.docudevs.ai/configuration/invoice-config \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

### Deleting a Configuration

Remove a configuration when it is no longer needed.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
await client.delete_configuration("invoice-config")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs delete-configuration invoice-config
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X DELETE https://api.docudevs.ai/configuration/invoice-config \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

## Using Configurations

Once saved, you can use a configuration to process documents. This applies all the settings defined in the configuration to the processing job.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

# Process document using the saved configuration
job_guid = await client.submit_and_process_document_with_configuration(
    document=document_data,
    document_mime_type="application/pdf",
    configuration_name="invoice-config"
)

# Wait for result
result = await client.wait_until_ready(job_guid)
print(result)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process invoice.pdf --configuration invoice-config
```

  </TabItem>
  <TabItem value="curl">

```bash
# Upload and process with configuration query parameter
curl -X POST "https://api.docudevs.ai/document/process/DOCUMENT_GUID?configuration=invoice-config" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{}'
```

  </TabItem>
</Tabs>

## Configuration Parameters

When creating a configuration, you can specify the following parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `prompt` | String | Custom instructions for data extraction. |
| `schema` | Object | JSON Schema defining the exact output structure. |
| `ocr` | Enum | OCR quality level (`NONE`, `DEFAULT`, `PREMIUM`). |
| `llm` | Enum | LLM model quality (`DEFAULT`, `MINI`, `HIGH`). |
| `extractionMode` | Enum | Extraction method (`SIMPLE`, `STEPS`). |
| `barcodes` | Boolean | Enable barcode and QR code scanning. |
| `describeFigures` | Boolean | Enable AI description of figures and images. |
| `extractFigures` | Boolean | Store figure images and metadata for later download. |

## Best Practices

- **Version Your Configurations**: If you change your schema significantly, consider creating a new configuration (e.g., `invoice-v2`) to avoid breaking existing integrations.
- **Use Schemas**: Always define a strict JSON schema to ensure predictable API responses.
- **Test Prompts**: Experiment with different prompts in the playground or via temporary configurations before finalizing them.
- **Optimize Costs**: Use `DEFAULT` OCR and LLM for simple documents and `HIGH` only when necessary.
