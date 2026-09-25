---
title: Bring Your Own OCR
description: Connect your organization's Azure Document Intelligence account for document OCR.
sidebar_position: 5
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

DocuDevs can extract text using your organization's own Azure Document
Intelligence (Azure OCR) account instead of the DocuDevs platform account.

:::note Who can manage OCR providers
Creating, editing, deleting, or key-binding OCR providers is not gated to
organization admins today: any member of the organization holding an API key
can perform these actions. This differs from
[embedding providers](/docs/administration/bring-your-own-embeddings) and
[model credentials](/docs/administration/model-credentials), which require the
`ADMIN` role for the same operations.
:::

OCR providers do not support shared model credentials: each provider stores
its own `apiKey` directly. This is different from LLM and embedding
providers, which can reference a reusable
[model credential](/docs/administration/model-credentials).

There is no dedicated verify/probe endpoint for OCR providers today, unlike
`POST /llm/providers/{id}/verify` for LLM providers. Confirm a provider works
by running a document-processing job against it.

## Configure a provider

1. Open **Settings → OCR Providers** and select **New**.
2. Enter a name, the Azure Document Intelligence endpoint, the API key, and
   the model ID (for example `prebuilt-document`).
3. Optionally set provider-specific `features`.
4. Save the provider.
5. Bind the provider to one or more logical OCR keys.

Only explicitly bound keys use the provider. Unbound keys continue to use the
DocuDevs platform OCR account.

## Create a provider

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
API_URL="https://api.docudevs.ai"
API_KEY="<your-api-key>"

curl -X POST "$API_URL/ocr/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Our Doc Intelligence",
    "endpoint": "https://myacct.cognitiveservices.azure.com",
    "apiKey": "<azure-doc-intelligence-key>",
    "modelId": "prebuilt-document"
  }'
```

  </TabItem>
  <TabItem value="python">

The hand-written `DocuDevsClient.create_ocr_provider`/`update_ocr_provider`
convenience methods send outdated field names (`model` instead of `modelId`)
and will be rejected by the API. Until they are fixed, call the generated,
typed functions directly:

```python
from docudevs.client import AuthenticatedClient
from docudevs.api.azure_ocr_providers.create_ocr_provider import asyncio_detailed as create_ocr_provider
from docudevs.models.create_ocr_provider_request import CreateOcrProviderRequest

client = AuthenticatedClient(base_url="https://api.docudevs.ai", token="<your-api-key>")

body = CreateOcrProviderRequest(
    name="Our Doc Intelligence",
    endpoint="https://myacct.cognitiveservices.azure.com",
    api_key="<azure-doc-intelligence-key>",
    model_id="prebuilt-document",
)
response = await create_ocr_provider(client=client, body=body)
provider = response.parsed
```

  </TabItem>
</Tabs>

## Update or delete a provider

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl -X PATCH "$API_URL/ocr/providers/7" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"modelId": "prebuilt-invoice"}'

curl -X DELETE "$API_URL/ocr/providers/7" -H "Authorization: $API_KEY"
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.api.azure_ocr_providers.update_ocr_provider import asyncio_detailed as update_ocr_provider
from docudevs.api.azure_ocr_providers.delete_ocr_provider import asyncio_detailed as delete_ocr_provider
from docudevs.models.update_ocr_provider_request import UpdateOcrProviderRequest

body = UpdateOcrProviderRequest(model_id="prebuilt-invoice")
await update_ocr_provider(id=7, client=client, body=body)

await delete_ocr_provider(id=7, client=client)
```

  </TabItem>
</Tabs>

## Key bindings

Each logical OCR key is bound to a provider independently. Listing and
binding keys use the correctly-mapped `list_ocr_keys`/`update_ocr_key_binding`
convenience methods on `DocuDevsClient`.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl "$API_URL/ocr/keys" -H "Authorization: $API_KEY"

curl -X PUT "$API_URL/ocr/keys/DEFAULT" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"providerId": 7}'
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs import DocuDevsClient

client = DocuDevsClient(api_url="https://api.docudevs.ai", token="<your-api-key>")

keys = (await client.list_ocr_keys()).json()

await client.update_ocr_key_binding("DEFAULT", 7)
# Pass provider_id=None to clear a binding
```

`list_ocr_keys`/`update_ocr_key_binding` call the API with a plain
`httpx.AsyncClient` under the hood and return a regular `httpx.Response`
(`.json()`, `.status_code`), unlike the generated functions used above.

  </TabItem>
</Tabs>

## LLM providers

Chat/extraction models are configured separately under
**Settings → LLM Providers** — see
[Bring your own LLM](/docs/administration/bring-your-own-llm). OCR and LLM
bindings are independent of each other.
