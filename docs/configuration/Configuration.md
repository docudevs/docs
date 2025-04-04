---
title: Named configurations
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Overview

Named configurations allow you to save and reuse processing settings across multiple document processing requests. This helps maintain consistency in your document processing pipeline and simplifies your code when you need to process many documents with the same settings.

With configurations, you can define settings like:
- OCR quality level
- LLM model selection
- Extraction mode
- Barcode and QR code scanning
- And other processing parameters

## Managing Configurations

### Saving a Configuration

You can save a new configuration or update an existing one by sending a POST request to `/configuration/{name}`. The name serves as a unique identifier for retrieving the configuration later.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X POST https://api.docudevs.ai/configuration/invoice-config \
     -H "Authorization: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "ocr": "PREMIUM",
       "llm": "PREMIUM", 
       "extractionMode": "STEPS",
       "barcodes": true
     }'
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Save a configuration with the name "invoice-config"
config = {
    "ocr": "PREMIUM",
    "llm": "PREMIUM",
    "extractionMode": "STEPS",
    "barcodes": True
}

result = await client.save_configuration("invoice-config", config)
print(f"Configuration saved: {result}")
```
  </TabItem>
</Tabs>

### Listing All Configurations

To see all your saved configurations, send a GET request to `/configuration`:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X GET https://api.docudevs.ai/configuration \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

configs = await client.list_configurations()
print(f"Available configurations: {configs}")
```
  </TabItem>
</Tabs>

### Retrieving a Specific Configuration

To get the details of a specific configuration, send a GET request to `/configuration/{name}`:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X GET https://api.docudevs.ai/configuration/invoice-config \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

config = await client.get_configuration("invoice-config")
print(f"Configuration details: {config}")
```
  </TabItem>
</Tabs>

### Deleting a Configuration

When you no longer need a configuration, you can delete it with a DELETE request to `/configuration/{name}`:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X DELETE https://api.docudevs.ai/configuration/invoice-config \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

await client.delete_configuration("invoice-config")
print("Configuration deleted")
```
  </TabItem>
</Tabs>

## Using Configurations

### When Processing a Document

You can apply a saved configuration when processing a document by specifying the configuration name in the request:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X POST "https://api.docudevs.ai/document/process/DOCUMENT_GUID?configuration=invoice-config" \
     -H "Authorization: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{}'
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

# First upload a document
with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_document(document=document, document_mime_type="application/pdf")

# Then process it with the saved configuration
result = await client.process_document(
    guid=guid,
    configuration="invoice-config"
)

print(result)
```
  </TabItem>
</Tabs>

## Configuration Parameters

When creating or updating a configuration, you can specify the following parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `ocr` | Enum | OCR quality level. Possible values: `NONE`, `DEFAULT`, `PREMIUM` |
| `llm` | Enum | LLM model quality. Possible values: `DEFAULT`, `PREMIUM` |
| `extractionMode` | Enum | Document extraction method. Possible values: `SIMPLE`, `STEPS` |
| `barcodes` | Boolean | Whether to scan for barcodes and QR codes |
| `skipProcessingAfterUpload` | Boolean | Whether to skip automatic processing after upload |

### Example: Configuration for Invoice Processing

```json
{
  "ocr": "DEFAULT",
  "llm": "DEFAULT", 
  "extractionMode": "SIMPLE",
  "barcodes": true
}
```


## Best Practices

1. **Naming conventions** - Use descriptive names for your configurations such as "invoice-premium" or "receipt-fast" to easily identify their purpose.

2. **Configuration reuse** - Create separate configurations for different document types or processing requirements rather than modifying a single configuration repeatedly.

3. **Optimization** - Only enable the features you need. For example, if you don't need barcode scanning, turn it off to improve processing speed.

4. **Testing** - Before widely deploying a configuration, test it with sample documents to ensure it meets your accuracy requirements.

5. **Versioning** - Include version information in your configuration names if you frequently update processing settings, e.g., "invoice-config-v2".

## Conclusion

Named configurations provide a powerful way to standardize your document processing workflow across different document types and use cases. By saving and reusing optimized processing settings, you can ensure consistent results while simplifying your integration code.
