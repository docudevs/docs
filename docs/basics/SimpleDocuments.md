---
title: Simple documents
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Principles

There are 4 items you can send to the API:

1. The document to be processed (mandatory)
2. The schema to be used (optional)
3. Processing instructions for the data (optional)
4. Configuration options (optional)

If you don't specify the schema or the configuration, the API will use sensible defaults for them.

## Example 1 - Invoice - minimal using just the document

![example invoice](/files/simple_documents/invoice-screenshot.png)

[Example invoice](/files/simple_documents/invoice.pdf). You can send it to the API using the following command:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```sh
curl -s -S -X POST https://api.docudevs.ai/document/upload-files \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf"
```

  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os
import json

# Initialize the client with your API key

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Process the document

with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_and_process_document(
  document=document,
  document_mime_type="application/pdf"
)
result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

```
  </TabItem>
  <TabItem value="cli">
```bash
docudevs process invoice.pdf
```

  </TabItem>
</Tabs>

This will give you the result of the document being processed using the default schema and configuration.
```json
{
  "invoice": {
    "header": "QR-bill - Swiss Payment Standards I SIX",
    "date": "24/10/2024",
    "time": "15:21",
    "company": {
      "name": "Max Muster & S\u00f6hne",
      "address": "Musterstrasse 123",
      "postal_code": "8000",
      "city": "Seldwyla",
      "phone": "012 345 67 89",
      "website": "www.qr-bill.ch"
    },
    "invoice_details": {
      "recipient": {
        "name": "Simon Muster",
        "address": "Musterstrasse 1",
        "postal_code": "8000",
        "city": "Seldwyla"
      },
      "specimen_date": "15 January 2021",
      "invoice_number": "999",
      "item_description": "Payment of travel",
      "price_per_unit": "CHF 46.40",
      "subtotal": "CHF 46.40",
      "vat": {
        "rate": "7.7%",
        "amount": "CHF 3.60"
      },
      "total": "CHF 50.00",
      "payment_terms": "Please transfer the invoiced amount within 30 days."
    },
    "receipt": {
      "account": {
        "iban": "CH64 3196 1000 0044 2155 7",
        "name": "Max Muster & S\u00f6hne",
        "address": "Musterstrasse 123",
        "postal_code": "8000",
        "city": "Seldwyla"
      },
      "reference": "00 00082 07791 22585 74212 86694",
      "payable_by": {
        "name": "Simon Muster",
        "address": "Musterstrasse 1",
        "postal_code": "8000",
        "city": "Seldwyla"
      },
      "currency": "CHF",
      "amount": "50.00",
      "additional_information": "Payment of travel"
    },
    "footer": "https://www.six-group.com/en/products-services/banking-services/payment-standardization/standards/qr-bill.html",
    "page_number": "1/10"
  }
}
```

## Example 2 - Invoice - provide instructions

Previous example provided a lot of data and fields. Maybe you only need few fields from the invoice?

You can create an instructions file and send it to the API. This file is free-formatted text, that you can
fill in any language.

Example of instructions file that we name `instructions.txt`:

```
I need: IBAN, reference-number, account-owner, 
account-address and total amount from this invoice.
```

Send the instructions together with the document to the api:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```sh
curl -s -S -X POST https://api.docudevs.ai/document/upload-files \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf" \
     -F "instructions=@instructions.txt"
```

  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os
import json

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Read instructions from file

with open('instructions.txt', 'r') as file:
    prompt = file.read()

# Process the document with instructions

with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_and_process_document(
    document=document,
    document_mime_type="application/pdf",
    prompt=prompt
)
result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

```
  </TabItem>
  <TabItem value="cli">
```bash
docudevs process invoice.pdf --schema-file schema.json
```

  </TabItem>
</Tabs>

This will give you the results that you specified in the instructions.txt:

```json
{
  "iban": "CH64 3196 1000 0044 2155 7",
  "reference_number": "00 00082 07791 22585 74212 86694",
  "account_owner": "Max Muster & S\u00f6hne",
  "account_address": "Musterstrasse 123 8000 Seldwyla",
  "total_amount": "CHF 50.00"
}
```

## Example 3 - Invoice - provide schema

Previous example provided nicely only the data fields that we are interested in.
But what if we want to have the data in a specific format? To integrate it with our system we need the data in a specific format.
For example the previous example gave us the total amount as a string "50.00 CHF". But we want to have it as a number.

The schema is given in [JSON-Schema](https://json-schema.org/) format.
To create a schema, you can just ask ChatGPT to do it for you from a given example.

Let's use the previous answer as an example. We want to have the total amount as a number, and
the address split into street, postal code and city.

The json-schema for this would look like this (as created by chat-gpt):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "iban": {
      "type": "string",
      "description": "International Bank Account Number",
      "pattern": "^[A-Z]{2}[0-9]{2} [0-9]{4} [0-9]{4} [0-9]{4} [0-9]{4} [0-9]$"
    },
    "reference_number": {
      "type": "string",
      "description": "Reference number for the transaction",
      "pattern": "^[0-9]{2} [0-9]{5} [0-9]{5} [0-9]{5} [0-9]{5} [0-9]{5}$"
    },
    "account_owner": {
      "type": "string",
      "description": "Name of the account owner"
    },
    "account_address": {
      "type": "object",
      "properties": {
        "street": {
          "type": "string",
          "description": "Street name and number of the account owner's address"
        },
        "postal_code": {
          "type": "string",
          "description": "Postal code of the account owner's address",
          "pattern": "^[0-9]{4}$"
        },
        "city": {
          "type": "string",
          "description": "City of the account owner's address"
        }
      },
      "required": ["street", "postal_code", "city"],
      "description": "Address of the account owner split into components"
    },
    "total_amount": {
      "type": "number",
      "description": "Total amount in CHF"
    }
  },
  "required": ["iban", "reference_number", "account_owner", "account_address", "total_amount"]
}
```

Let's store this schema in a file called `schema.json` and send it to the API together with the document:

**Note we don't need to specify the instructions in this case**

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```sh
curl -s -S -X POST https://api.docudevs.ai/document/upload-files/sync \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf" \
     -F "schema=@schema.json"
```

  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os
import json

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Read schema from file

with open('schema.json', 'r') as file:
    schema = json.load(file)

# Process the document with schema

with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_and_process_document(
    document=document,
    document_mime_type="application/pdf",
    schema=schema
)
result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

```
  </TabItem>
  <TabItem value="cli">
```bash
# Prompt text comes from instructions.txt; --barcodes enables QR detection
docudevs process invoice.pdf \
  --prompt-file instructions.txt \
  --barcodes
```

  </TabItem>
</Tabs>

This will give you the results:

```json
{
  "iban": "CH64 3196 1000 0044 2155 7",
  "reference_number": "00 00082 07791 22585 74212 86694",
  "account_owner": "Max Muster & S\u00f6hne",
  "account_address": {
    "street": "Musterstrasse 123",
    "postal_code": "8000",
    "city": "Seldwyla"
  },
  "total_amount": 50.0
}
```

## Example 4 - Invoice - QR Code

The previous examples showed how to extract data from an invoice. But what if you want to extract the QR code as well?

For this we would need to set a configuration parameter. The configuration is a JSON object that can contain various settings for the processing.

we create a configuration file `config.json` with the following content:

```json
{
  "barcodes": true
}
```

Let's have an instructions file `instructions.txt` that say we only want the QR code:

```
I need: QR code from this invoice.
```

Let's send them to the API together with the document:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```sh
curl -s -S -X POST https://api.docudevs.ai/document/upload-files/sync \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf" \
     -F "instructions=@instructions.txt" \
     -F "metadata=@config.json"
```

  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os
import json

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Read instructions from file

with open('instructions.txt', 'r') as file:
    prompt = file.read()

# Process the document with instructions and config

with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_and_process_document(
    document=document,
    document_mime_type="application/pdf",
    prompt=prompt,
    barcodes=True
)
result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

```
  </TabItem>
  <TabItem value="cli">
```bash
docudevs process invoice.pdf \
  --prompt-file instructions.txt \
  --barcodes
```

  </TabItem>
</Tabs>

This will give you the results:

```json
{
  "QRCode": [
    "SPC",
    "0200",
    "1",
    "CH6431961000004421557",
    "S",
    "Max Muster & S\u00f6hne",
    "Musterstrasse",
    "123",
    "8000",
    "Seldwyla",
    "CH",
    "",
    "",
    "",
    "",
    "",
    "",
    "",
    "50.00",
    "CHF",
    "S",
    "Simon Muster",
    "Musterstrasse",
    "1",
    "8000",
    "Seldwyla",
    "CH",
    "QRR",
    "000008207791225857421286694",
    "Payment of travel",
    "EPD"
  ]
}
```

## Example 5 - Using Pydantic models for schema definition

Pydantic is a popular Python library for data validation and settings management using Python type annotations. You can use Pydantic models to define your schema and then convert it to JSON Schema for use with DocuDevs API.

First, let's define a Pydantic model for our invoice data:

```python
from pydantic import BaseModel, Field

class AccountAddress(BaseModel):
    street: str = Field(description="Street name and number of the account owner's address")
    postal_code: str = Field(description="Postal code of the account owner's address", pattern="^[0-9]{4}$")
    city: str = Field(description="City of the account owner's address")

class InvoiceData(BaseModel):
    iban: str = Field(
        description="International Bank Account Number",
        pattern="^[A-Z]{2}[0-9]{2} [0-9]{4} [0-9]{4} [0-9]{4} [0-9]{4} [0-9]$"
    )
    reference_number: str = Field(
        description="Reference number for the transaction",
        pattern="^[0-9]{2} [0-9]{5} [0-9]{5} [0-9]{5} [0-9]{5} [0-9]{5}$"
    )
    account_owner: str = Field(description="Name of the account owner")
    account_address: AccountAddress = Field(description="Address of the account owner split into components")
    total_amount: float = Field(description="Total amount in CHF")
```

Now, let's use this model to generate a JSON schema and process our document:

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import json
from pydantic import BaseModel, Field

# Define your Pydantic models here (as shown above)

# Initialize the client
client = DocuDevsClient(token=os.getenv('API_KEY'))

# Convert Pydantic model to JSON schema
schema = json.dumps(InvoiceData.model_json_schema())

# Process the document with the schema
with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_and_process_document(
    document=document, 
    document_mime_type="application/pdf",
    schema=schema
)
result = await client.wait_until_ready(guid, result_format="json")

# Convert response directly to Pydantic model
invoice_data = InvoiceData.model_validate(result)

# Now you can access the data in a type-safe manner
print(f"IBAN: {invoice_data.iban}")
print(f"Total Amount: {invoice_data.total_amount}")
print(f"City: {invoice_data.account_address.city}")
```

  </TabItem>
</Tabs>

Using Pydantic models provides several advantages:

1. **Type safety** - You get autocomplete and type checking in your IDE
2. **Data validation** - Pydantic validates that the response matches your expected structure
3. **Documentation** - The schema is self-documenting through Python type annotations
4. **Integration** - Seamlessly works with other tools in the Python ecosystem

This approach is particularly useful when integrating document processing into larger applications where type safety and data validation are important.
