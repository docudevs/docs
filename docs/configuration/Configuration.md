---
title: Named configurations
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Overview

Named configurations allow you to save and reuse processing settings across multiple document processing requests. This helps maintain consistency in your document processing pipeline and simplifies your code when you need to process many documents with the same settings.

**🎯 Key Feature: Prompts & Schemas**

The most powerful aspect of named configurations is the ability to store **prompts** and **schemas**. This ensures:

- **Consistent data extraction** across all documents processed with the same configuration
- **Standardized response format** - all processed documents return data in the exact same structure
- **Reusable business logic** - define once, use everywhere

With configurations, you can define:

### Core Processing Settings

- **Prompt** - Custom instructions for data extraction
- **Schema** - JSON schema defining the exact output structure ⭐ *Most Important*
- OCR quality level
- LLM model selection
- Extraction mode
- Barcode and QR code scanning
- And other processing parameters

### Why Schemas Matter

When you include a schema in your named configuration, **every document** processed with that configuration will return data in the exact same format. This is crucial for:

- **Database integration** - consistent field names and types
- **API consumption** - predictable response structure
- **Business logic** - reliable data processing workflows
- **Data analysis** - standardized data for reporting and analytics

## Managing Configurations

### Saving a Configuration

You can save a new configuration or update an existing one by sending a POST request to `/configuration/{name}`. The name serves as a unique identifier for retrieving the configuration later.

#### Example: Complete Invoice Configuration with Schema

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```sh
curl -s -S -X POST https://api.docudevs.ai/configuration/invoice-config \
     -H "Authorization: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "prompt": "Extract invoice data including vendor details, line items, and totals. Pay special attention to tax calculations and invoice dates.",
       "schema": {
         "type": "object",
         "properties": {
           "vendor_name": {"type": "string"},
           "invoice_number": {"type": "string"},
           "invoice_date": {"type": "string", "format": "date"},
           "due_date": {"type": "string", "format": "date"},
           "total_amount": {"type": "number"},
           "tax_amount": {"type": "number"},
           "line_items": {
             "type": "array",
             "items": {
               "type": "object",
               "properties": {
                 "description": {"type": "string"},
                 "quantity": {"type": "number"},
                 "unit_price": {"type": "number"},
                 "total": {"type": "number"}
               }
             }
           }
         }
       },
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

# Save a comprehensive configuration with prompt and schema

config = {
    "prompt": "Extract invoice data including vendor details, line items, and totals. Pay special attention to tax calculations and invoice dates.",
    "schema": {
        "type": "object",
        "properties": {
            "vendor_name": {"type": "string"},
            "invoice_number": {"type": "string"},
            "invoice_date": {"type": "string", "format": "date"},
            "due_date": {"type": "string", "format": "date"},
            "total_amount": {"type": "number"},
            "tax_amount": {"type": "number"},
            "line_items": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "description": {"type": "string"},
                        "quantity": {"type": "number"},
                        "unit_price": {"type": "number"},
                        "total": {"type": "number"}
                    }
                }
            }
        }
    },
    "ocr": "PREMIUM",
    "llm": "PREMIUM",
    "extractionMode": "STEPS",
    "barcodes": True
}

result = await client.save_configuration("invoice-config", config)
print(f"Configuration saved: {result}")

```
  </TabItem>
  <TabItem value="cli">
```bash
# Save the configuration JSON above to config.json first
docudevs save-configuration invoice-config config.json
```

  </TabItem>
</Tabs>

#### Simple Configuration (Processing Settings Only)

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X POST https://api.docudevs.ai/configuration/simple-config \
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

# Save a configuration with processing settings only

config = {
    "ocr": "PREMIUM",
    "llm": "PREMIUM",
    "extractionMode": "STEPS",
    "barcodes": True
}

result = await client.save_configuration("simple-config", config)
print(f"Configuration saved: {result}")

```
  </TabItem>
  <TabItem value="cli">
```bash
# Save the JSON payload above to simple-config.json first
docudevs save-configuration simple-config simple-config.json
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
    {label: 'CLI', value: 'cli'},
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
  <TabItem value="cli">
```bash
docudevs list-configurations
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
    {label: 'CLI', value: 'cli'},
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
  <TabItem value="cli">
```bash
docudevs get-configuration invoice-config
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
    {label: 'CLI', value: 'cli'},
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
  <TabItem value="cli">
```bash
docudevs delete-configuration invoice-config
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
    {label: 'CLI', value: 'cli'},
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
import json

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Upload and process in one step with a saved configuration

with open("invoice.pdf", "rb") as f:
  document = f.read()

guid = await client.submit_and_process_document_with_configuration(
  document=document,
  document_mime_type="application/pdf",
  configuration_name="invoice-config"
)

result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

```
  </TabItem>
  <TabItem value="cli">
```bash
docudevs process invoice.pdf --configuration invoice-config
```

  </TabItem>
</Tabs>

## Configuration Parameters

When creating or updating a configuration, you can specify the following parameters:

### Content & Structure Parameters

| Parameter | Type | Description |
| --- | --- | --- |
| `prompt` | String | **Custom instructions** for data extraction. Define what information to extract and any specific requirements. |
| `schema` | Object | **JSON Schema** defining the exact output structure. ⭐ **Critical for consistent response format** |

### Processing Parameters

| Parameter | Type | Description |
| --- | --- | --- |
| `ocr` | Enum | OCR quality level. Possible values: `NONE`, `DEFAULT`, `PREMIUM` |
| `llm` | Enum | LLM model quality. Possible values: `DEFAULT`, `PREMIUM` |
| `extractionMode` | Enum | Document extraction method. Possible values: `SIMPLE`, `STEPS` |
| `barcodes` | Boolean | Whether to scan for barcodes and QR codes |
| `skipProcessingAfterUpload` | Boolean | Whether to skip automatic processing after upload |

### Schema Example: Invoice Processing

```json
{
  "prompt": "Extract all invoice details with focus on accuracy of financial amounts",
  "schema": {
    "type": "object",
    "properties": {
      "vendor_name": {"type": "string"},
      "invoice_number": {"type": "string"},
      "invoice_date": {"type": "string", "format": "date"},
      "due_date": {"type": "string", "format": "date"},
      "total_amount": {"type": "number"},
      "tax_amount": {"type": "number"},
      "currency": {"type": "string"},
      "line_items": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "description": {"type": "string"},
            "quantity": {"type": "number"},
            "unit_price": {"type": "number"},
            "total": {"type": "number"}
          },
          "required": ["description", "quantity", "unit_price", "total"]
        }
      }
    },
    "required": ["vendor_name", "invoice_number", "total_amount"]
  },
  "ocr": "DEFAULT",
  "llm": "DEFAULT", 
  "extractionMode": "SIMPLE",
  "barcodes": true
}
```

### Schema Example: Receipt Processing

```json
{
  "prompt": "Extract receipt information focusing on merchant details and purchased items",
  "schema": {
    "type": "object",
    "properties": {
      "merchant_name": {"type": "string"},
      "transaction_date": {"type": "string", "format": "date-time"},
      "total_amount": {"type": "number"},
      "payment_method": {"type": "string"},
      "items": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "price": {"type": "number"}
          }
        }
      }
    },
    "required": ["merchant_name", "total_amount"]
  },
  "ocr": "PREMIUM",
  "llm": "DEFAULT"
}
```

## Best Practices

### Schema Design

1. **Always define a schema** - This is the most important part of a configuration. Without a schema, response formats may vary between documents.

2. **Use required fields** - Mark essential fields as required in your schema to ensure they're always extracted.

3. **Choose appropriate data types** - Use `number` for amounts, `date` for dates, `string` for text, and `array` for collections.

4. **Nest objects logically** - Group related fields (like `line_items` in invoices) into nested objects or arrays.

### Prompt Optimization

1. **Be specific** - Clear, detailed prompts lead to better extraction results.

2. **Include context** - Mention the document type and any special requirements in your prompt.

3. **Test iteratively** - Start with basic prompts and refine based on results.

### Configuration Management

1. **Naming conventions** - Use descriptive names for your configurations such as "invoice-premium" or "receipt-fast" to easily identify their purpose.

2. **Configuration reuse** - Create separate configurations for different document types or processing requirements rather than modifying a single configuration repeatedly.

3. **Optimization** - Only enable the features you need. For example, if you don't need barcode scanning, turn it off to improve processing speed.

4. **Testing** - Before widely deploying a configuration, test it with sample documents to ensure it meets your accuracy requirements.

5. **Versioning** - Include version information in your configuration names if you frequently update processing settings, e.g., "invoice-config-v2".

## Real-World Examples

### E-commerce Receipt Processing

```json
{
  "prompt": "Extract purchase details from e-commerce receipts including order info and shipping",
  "schema": {
    "type": "object",
    "properties": {
      "order_number": {"type": "string"},
      "purchase_date": {"type": "string", "format": "date"},
      "customer_email": {"type": "string", "format": "email"},
      "total_amount": {"type": "number"},
      "shipping_cost": {"type": "number"},
      "items": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "product_name": {"type": "string"},
            "quantity": {"type": "integer"},
            "unit_price": {"type": "number"}
          }
        }
      }
    }
  }
}
```

### Healthcare Form Processing

```json
{
  "prompt": "Extract patient information and medical details from healthcare forms",
  "schema": {
    "type": "object",
    "properties": {
      "patient_name": {"type": "string"},
      "date_of_birth": {"type": "string", "format": "date"},
      "medical_record_number": {"type": "string"},
      "diagnosis_codes": {
        "type": "array",
        "items": {"type": "string"}
      },
      "medications": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "dosage": {"type": "string"},
            "frequency": {"type": "string"}
          }
        }
      }
    }
  }
}

## Conclusion

Named configurations provide a powerful way to standardize your document processing workflow across different document types and use cases. By combining **prompts** and **schemas** with processing settings, you can ensure consistent, predictable results while simplifying your integration code.

**Key Takeaways:**
- **Schemas guarantee consistent response formats** - essential for reliable data integration
- **Prompts improve extraction accuracy** - provide context and specific instructions
- **Reusable configurations** save development time and ensure consistency
- **Processing settings** optimize performance for your specific needs

Start by creating configurations with schemas for your most common document types, and you'll see immediate benefits in data consistency and processing reliability.
