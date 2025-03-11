---
title: Templates
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Templates functionality allows you to upload, manage, and fill templates with data. Templates can be in PDF or Office (word, excel or powerpoint) format.

### Upload Template

**Endpoint:** `POST /template/{name}`

**Description:** Uploads a new template document (PDF or Word) to the server.

**Request:**
- **Path Parameter:**
    - `name` (String): The name of the template.
- **Headers:**
    - `Authorization` (String): API key for authorization.
- **Form Data:**
    - `document` (File): The template document to be uploaded.

**Response:**
- **200 OK:** Template uploaded successfully.
- **400 Bad Request:** Invalid input.
- **500 Internal Server Error:** Server error.

**Example:**
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -X POST https://api.docudevs.ai/template/invoice \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf"
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
from docudevs_client import DocuDevsClient

api_key = 'YOUR_API_KEY'
template_name = 'invoice'
client = DocuDevsClient(token=api_key)

async def upload_template():
    with open('invoice.pdf', 'rb') as file:
        response = await client.upload_template(
            name=template_name, 
            file=file
        )
        if response.status_code == 200:
            print("Template uploaded successfully")
            print(response.parsed)  # Contains form fields and template info
        else:
            print(f"Error: {response.status_code}")

asyncio.run(upload_template())
```
  </TabItem>
</Tabs>

Response example:
```json
{
  "guid": "123e4567-e89b-12d3-a456-426614174000",
  "formFields": [
    {
      "name": "customerName",
      "tooltip": "Enter the customer's name",
      "type": "text",
      "flags": ["required"],
      "value": "",
      "defaultValue": "John Doe",
      "options": [],
      "script": null
    },
    {
      "name": "invoiceAmount",
      "tooltip": "Enter the invoice amount",
      "type": "text",
      "flags": ["required"],
      "value": "",
      "defaultValue": "100.00",
      "options": [],
      "script": null
    },
    {
      "name": "paymentStatus",
      "tooltip": "",
      "type": "CHECK_BOX",
      "flags": ["COMBO_BOX"],
      "value": "Paid",
      "defaultValue": "Paid",
      "options": ["Paid", "Unpaid"],
      "script": null
    },
    {
      "name": "termsAccepted",
      "tooltip": null,
      "type": "PUSH_BUTTON",
      "flags": [],
      "value": "UNCHECKED",
      "defaultValue": null,
      "options": [],
      "script": null
    }
  ]
}
```

### Get Template Metadata

**Endpoint:** `GET /template/metadata/{name}`

**Description:** Retrieves metadata for a specific template.

**Request:**
- **Path Parameter:**
    - `name` (String): The name of the template.
- **Headers:**
    - `Authorization` (String): API key for authorization.

**Response:**
- **200 OK:** Returns the metadata of the template.
- **404 Not Found:** Template not found.
- **500 Internal Server Error:** Server error.

**Example:**
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -X GET https://api.docudevs.ai/template/metadata/invoice \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
from docudevs_client import DocuDevsClient

api_key = 'YOUR_API_KEY'
template_name = 'invoice'
client = DocuDevsClient(token=api_key)

async def get_template_metadata():
    response = await client.metadata(template_id=template_name)
    if response.status_code == 200:
        print("Template metadata retrieved successfully")
        print(response.parsed)  # Contains template metadata
    else:
        print(f"Error: {response.status_code}")

asyncio.run(get_template_metadata())
```
  </TabItem>
</Tabs>

### Delete Template

**Endpoint:** `DELETE /template/{name}`

**Description:** Deletes a specific template.

**Request:**
- **Path Parameter:**
    - `name` (String): The name of the template.
- **Headers:**
    - `Authorization` (String): API key for authorization.

**Response:**
- **200 OK:** Template deleted successfully.
- **404 Not Found:** Template not found.
- **500 Internal Server Error:** Server error.

**Example:**
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -X DELETE https://api.docudevs.ai/template/invoice \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
from docudevs_client import DocuDevsClient

api_key = 'YOUR_API_KEY'
template_name = 'invoice'
client = DocuDevsClient(token=api_key)

async def delete_template():
    response = await client.delete_template(template_id=template_name)
    if response.status_code == 200:
        print(f"Template {template_name} deleted successfully")
    else:
        print(f"Error deleting template: {response.status_code}")

asyncio.run(delete_template())
```
  </TabItem>
</Tabs>

### Fill Template

**Endpoint:** `POST /template/fill/{name}`

**Description:** Fills a template with provided data and returns the filled document.

**Request:**
- **Path Parameter:**
    - `name` (String): The name of the template.
- **Headers:**
    - `Authorization` (String): API key for authorization.
- **Body:**
    - `fields` (JSON): A JSON object containing the fields to fill in the template.


The fields object must be a 'flat' dictionary for pdf-forms. It can be a nested object for word documents.

**Response:**
- **200 OK:** Returns the filled document.
- **404 Not Found:** Template not found.
- **500 Internal Server Error:** Server error.

**Example flat:**
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl --output filled_invoice.pdf -X POST https://api.docudevs.ai/template/fill/invoice \
     -H "Authorization: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"fields":{"name": "John Doe", "amount": "100.00", "termsAccepted": true}}'
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
import os
from docudevs_client import DocuDevsClient, TemplateFillRequest

api_key = 'YOUR_API_KEY'
template_name = 'invoice'
client = DocuDevsClient(token=api_key)

async def fill_template():
    # Create fields object with data to fill the template
    fields = {
        'name': 'John Doe',
        'amount': '100.00',
        'termsAccepted': True
    }
    
    # Create the template fill request
    fill_request = TemplateFillRequest(fields=fields)
    
    # Send the fill request
    response = await client.fill(name=template_name, body=fill_request)
    
    if response.status_code == 200:
        # Save the response to a file
        with open('filled_invoice.pdf', 'wb') as f:
            f.write(response.content)
        print("Template filled and saved successfully")
    else:
        print(f"Error filling template: {response.status_code}")

asyncio.run(fill_template())
```
  </TabItem>
</Tabs>

**Example word template:**
![img.png](./img.png)
Office templates can have nested objects. Creating of dynamic tables is also possible (demonstrated in the [example template](https://docs.docudevs.ai/template1.docx))

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl --output template-filled.docx -X POST https://api.docudevs.ai/template/fill/template1 \
     -H "Authorization: $API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "fields": {
         "bullets": ["item 1", "item 2"],
         "variable1": "value for variable 1",
         "variable2": "This is some text.",
         "col_labels": ["mammal", "bird", "reptile", "fish"],
         "tbl_contents": [
           {"label": "domestic", "cols": ["dog", "chicken", "turtle", "goldfish"]},
           {"label": "wild", "cols": ["lion", "eagle", "snake", "shark"]},
           {"label": "farm", "cols": ["cow", "duck", "lizard", "trout"]}
         ]
       }}'
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
from docudevs_client import DocuDevsClient, TemplateFillRequest

api_key = 'YOUR_API_KEY'
template_name = 'template1'
client = DocuDevsClient(token=api_key)

async def fill_complex_template():
    # Create fields object with nested data structure
    fields = {
        'bullets': ['item 1', 'item 2'],
        'variable1': 'value for variable 1',
        'variable2': 'This is some text.',
        'col_labels': ['mammal', 'bird', 'reptile', 'fish'],
        'tbl_contents': [
            {'label': 'domestic', 'cols': ['dog', 'chicken', 'turtle', 'goldfish']},
            {'label': 'wild', 'cols': ['lion', 'eagle', 'snake', 'shark']},
            {'label': 'farm', 'cols': ['cow', 'duck', 'lizard', 'trout']}
        ]
    }
    
    # Create the template fill request
    fill_request = TemplateFillRequest(fields=fields)
    
    # Send the fill request
    response = await client.fill(name=template_name, body=fill_request)
    
    if response.status_code == 200:
        # Save the response to a file
        with open('template-filled.docx', 'wb') as f:
            f.write(response.content)
        print("Complex template filled and saved successfully")
    else:
        print(f"Error filling template: {response.status_code}")

asyncio.run(fill_complex_template())
```
  </TabItem>
</Tabs>

Will render:
![img_1.png](./img_1.png)

### List Templates

**Endpoint:** `GET /template/list`

**Description:** Lists all templates for the organization.

**Request:**
- **Headers:**
    - `Authorization` (String): API key for authorization.

**Response:**
- **200 OK:** Returns a list of templates.
- **500 Internal Server Error:** Server error.

**Example:**
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -X GET https://api.docudevs.ai/template/list \
     -H "Authorization: $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
import asyncio
from docudevs_client import DocuDevsClient

api_key = 'YOUR_API_KEY'
client = DocuDevsClient(token=api_key)

async def list_templates():
    response = await client.list_templates()
    if response.status_code == 200:
        print("Templates retrieved successfully:")
        for template in response.parsed:
            print(f"- {template['name']}")
    else:
        print(f"Error retrieving templates: {response.status_code}")

asyncio.run(list_templates())
```
  </TabItem>
</Tabs>
