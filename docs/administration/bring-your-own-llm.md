---
title: Bring Your Own LLM
description: Connect your organization's Azure OpenAI, OpenAI, or OpenAI-compatible account for chat/extraction models.
sidebar_position: 4
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

DocuDevs can process documents using your organization's Azure OpenAI,
OpenAI, or OpenAI-compatible account. Azure OpenAI and OpenAI are fully
supported. OpenAI-compatible endpoints are supported best-effort through
the Chat Completions API.

:::note Who can manage LLM providers
Creating, editing, deleting, verifying, or key-binding LLM providers is not
gated to organization admins today: any member of the organization holding
an API key can perform these actions. This differs from
[embedding providers](/docs/administration/bring-your-own-embeddings) and
[model credentials](/docs/administration/model-credentials), which require the
`ADMIN` role for the same operations.
:::

## Configure a provider

1. Open **Settings → LLM Providers** and select **New**.
2. Select the provider type: `azure-openai`, `openai`, or `openai-compatible`.
3. For Azure OpenAI, enter the Azure endpoint and deployment name.
4. For OpenAI, enter the model name (for example `gpt-4o`) as the deployment
   name. DocuDevs uses OpenAI's default API URL, so the API URL field can be
   left blank.
5. For OpenAI-compatible, enter the base URL and model name.
6. Select a shared model credential, or enter an API key to create a
   dedicated credential, and save the provider.
7. Bind the provider to one or more levels: `DEFAULT`, `NANO`, `MINI`, or
   `HIGH`.

Only explicitly bound levels use the provider. Unbound levels continue to use
DocuDevs platform models.

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

curl -X POST "$API_URL/llm/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Our Azure OpenAI",
    "apiUrl": "https://myacct.openai.azure.com",
    "modelType": "azure-openai",
    "deploymentName": "gpt-4o",
    "credentialId": 42
  }'
```

  </TabItem>
  <TabItem value="python">

The hand-written `DocuDevsClient.create_llm_provider`/`update_llm_provider`
convenience methods send outdated field names and will be rejected by the
API. Until they are fixed, call the generated, typed functions directly:

```python
from docudevs.client import AuthenticatedClient
from docudevs.api.llm_providers.create_llm_provider import asyncio_detailed as create_llm_provider
from docudevs.models.create_llm_provider_request import CreateLlmProviderRequest

client = AuthenticatedClient(base_url="https://api.docudevs.ai", token="<your-api-key>")

body = CreateLlmProviderRequest(
    name="Our Azure OpenAI",
    api_url="https://myacct.openai.azure.com",
    model_type="azure-openai",
    deployment_name="gpt-4o",
    credential_id=42,
)
response = await create_llm_provider(client=client, body=body)
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
curl -X PATCH "$API_URL/llm/providers/7" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"deploymentName": "gpt-4o-mini"}'

curl -X DELETE "$API_URL/llm/providers/7" -H "Authorization: $API_KEY"
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.api.llm_providers.update_llm_provider import asyncio_detailed as update_llm_provider
from docudevs.api.llm_providers.delete_llm_provider import asyncio_detailed as delete_llm_provider
from docudevs.models.update_llm_provider_request import UpdateLlmProviderRequest

body = UpdateLlmProviderRequest(deployment_name="gpt-4o-mini")
await update_llm_provider(id=7, client=client, body=body)

await delete_llm_provider(id=7, client=client)
```

  </TabItem>
</Tabs>

## Verify credentials

Run a document-processing job after binding the provider. The real job is the
credential check and exercises the same structured-output, vision, streaming,
and usage path as production work. Invalid credentials or configuration fail
the affected job; DocuDevs does not silently reroute it to a platform model.

You can also probe a provider directly with **Verify provider**, which calls
the configured chat endpoint and checks that the token, gateway
authorization, deployment, and model settings work together:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl -X POST "$API_URL/llm/providers/7/verify" -H "Authorization: $API_KEY"
# -> 202 Accepted
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.api.llm_providers.verify_llm_provider import asyncio_detailed as verify_llm_provider

response = await verify_llm_provider(id=7, client=client)
```

  </TabItem>
</Tabs>

Shared credentials can use either an API key or a Microsoft Entra client
ID/client secret. See [Model credentials](/docs/administration/model-credentials)
for the Entra app-registration prerequisites, rotation, validation, and
rollout constraints. The same shared credential can be selected by chat and
embedding providers.

Provider secrets are write-only in the public API and are not returned to the UI.
Usage through your own provider is tracked for observability but is not charged
against your DocuDevs token balance.

For a shared credential, **Validate credential** proves that DocuDevs can decrypt
an API key or acquire an Entra access token. It does not call the model gateway.
Use **Verify provider** to call the configured chat endpoint and verify that the
token, gateway authorization, deployment, and model settings work together.
Either check fails closed; DocuDevs does not fall back to a platform model.

## Key bindings

Each logical level (`DEFAULT`, `NANO`, `MINI`, `HIGH`) is bound to a provider
independently. Listing and binding keys use the correctly-mapped
`list_llm_keys`/`update_llm_key_binding` convenience methods on
`DocuDevsClient`.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl "$API_URL/llm/keys" -H "Authorization: $API_KEY"

curl -X PUT "$API_URL/llm/keys/HIGH" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"providerId": 7}'
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs import DocuDevsClient

client = DocuDevsClient(api_url="https://api.docudevs.ai", token="<your-api-key>")

keys = (await client.list_llm_keys()).json()

await client.update_llm_key_binding("HIGH", 7)
# Pass provider_id=None to clear a binding
```

`list_llm_keys`/`update_llm_key_binding` call the API with a plain
`httpx.AsyncClient` under the hood and return a regular `httpx.Response`
(`.json()`, `.status_code`), unlike the generated functions used above.

  </TabItem>
</Tabs>

## Override reasoning effort for one job

The Python SDK accepts an optional `reasoning_effort` keyword on document-processing
helpers. It overrides the selected model level's configured effort for the entire job,
including pipeline steps and map-reduce chunks:

```python
guid = await client.submit_and_process_document(
    document=document,
    document_mime_type="application/pdf",
    prompt="Extract the contract terms.",
    llm="HIGH",
    reasoning_effort="low",
)
```

Omit the keyword to retain the built-in model level or BYOM provider default. Accepted
values depend on the selected model and provider; blank values are rejected.

In the Java SDK, set the same override with
`ProcessOptions.builder().reasoningEffort("low")`.

## Embeddings

Knowledge-base embeddings are configured separately under
**Settings → Embedding Providers** — see
[Bring your own embeddings](/docs/administration/bring-your-own-embeddings).
Chat and embedding bindings are independent: changing an LLM provider never
changes how your knowledge bases are embedded, and vice versa.
