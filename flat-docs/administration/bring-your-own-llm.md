---
title: Bring Your Own LLM
description: Connect your organization's Azure OpenAI, OpenAI, or OpenAI-compatible account for chat/extraction models.
sidebar_position: 4
---
<!-- Generated from docs/administration/bring-your-own-llm.md. Do not edit by hand. -->

DocuDevs can process documents using your organization's Azure OpenAI,
OpenAI, or OpenAI-compatible account. Azure OpenAI and OpenAI are fully
supported. OpenAI-compatible endpoints are supported best-effort through
the Chat Completions API.

:::note Who can manage LLM providers
Creating, editing, deleting, verifying, or key-binding LLM providers is not
restricted to organization admins: any member of the organization and any
organization API key can perform these actions. This differs from
[embedding providers](https://docs.docudevs.ai/docs/administration/bring-your-own-embeddings) and
[model credentials](https://docs.docudevs.ai/docs/administration/model-credentials), which require the
`ADMIN` role for the same operations.
:::

## Configure a provider

1. Open **Settings → LLM Providers** and select **New**.
2. Select the provider type: `azure-openai`, `openai`, or `openai-compatible`.
3. For Azure OpenAI, enter the Azure endpoint and deployment name.
4. For OpenAI, enter the model name (for example `gpt-4o`) as the deployment
   name. The form has no API URL field for this type and sends none. That works
   when you enter an API key in the form; a provider that uses a shared model
   credential needs an `https` `apiUrl`, so create it through the API.
5. For OpenAI-compatible, enter the base URL and model name.
6. Select a shared model credential or enter an API key to create a dedicated
   credential, and save the provider.
7. Bind the provider to one or more levels: `DEFAULT`, `NANO`, `MINI`, or
   `HIGH`.

Only explicitly bound levels use the provider. Unbound levels continue to use
DocuDevs platform models.

## Create a provider

Create a provider that uses a shared [model credential](https://docs.docudevs.ai/docs/administration/model-credentials). `modelType` is `azure-openai`, `openai` or `openai-compatible`. `deploymentName` is required (the model name for `openai`), and a provider that uses a credential needs an `https` `apiUrl`.

**cURL**

```bash
# CREDENTIAL_ID is the "id" of a model credential (see Model credentials).
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/llm/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson credentialId "$CREDENTIAL_ID" '{
    name: "Our Azure OpenAI",
    apiUrl: "https://example.openai.azure.com",
    modelType: "azure-openai",
    deploymentName: "gpt-4o",
    credentialId: $credentialId
  }')"
```

The Python SDK's provider methods send field names the API rejects on create; on update only `name` applies and the other fields are ignored, so use cURL for these steps. The CLI's create and update commands call the same methods.

## Update or delete a provider

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X PATCH "https://api.docudevs.ai/llm/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"deploymentName": "gpt-4o-mini"}'
```

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X DELETE "https://api.docudevs.ai/llm/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
```

**Python**

```python
# provider_id is the "id" returned when the provider was created or listed.
provider_id = 7

response = await client.delete_llm_provider(provider_id)
if response.status_code != 204:
    raise SystemExit(f"Delete failed: {response.status_code}")

print(f"HTTP {response.status_code}")
```

**CLI**

```bash
# 7 is the provider "id" returned when the provider was created or listed
docudevs llm delete 7
```

## Verify credentials

Run a document-processing job after binding the provider. The real job is the
credential check and exercises the same structured-output, vision, streaming,
and usage path as production work. Invalid credentials or configuration fail
the affected job; DocuDevs does not silently reroute it to a platform model.

You can also probe a provider directly with **Verify provider**, which calls
the configured chat endpoint and checks that the token, gateway
authorization, deployment, and model settings work together:

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

# 202 Accepted: the probe runs in the background
curl -s -S -f -X POST "https://api.docudevs.ai/llm/providers/$PROVIDER_ID/verify" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
```

The Python SDK has no method for verifying a provider, so this example is cURL only.

Shared credentials can use either an API key or a Microsoft Entra client
ID/client secret. See [Model credentials](https://docs.docudevs.ai/docs/administration/model-credentials)
for the Entra app-registration prerequisites, rotation, and validation. The same shared credential can be selected by chat and
embedding providers.

Provider secrets are write-only in the public API and are not returned to the UI.
Usage through your own provider is tracked for observability but is not charged
against your DocuDevs credit balance. The per-job fee still applies; see
[Billing and credits](https://docs.docudevs.ai/docs/reference/billing-credits).

For a shared credential, **Validate credential** proves that DocuDevs can decrypt
an API key or acquire an Entra access token. It does not call the model gateway.
Use **Verify provider** to call the configured chat endpoint and verify that the
token, gateway authorization, deployment, and model settings work together.
Either check fails closed; DocuDevs does not fall back to a platform model.

## Key bindings

Each logical level (`DEFAULT`, `NANO`, `MINI`, `HIGH`) is bound to a provider
independently. Binding a level to a provider, or clearing it, takes the provider `id`.

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

# List the current bindings of DEFAULT, NANO, MINI and HIGH
curl -s -S -f "https://api.docudevs.ai/llm/keys" \
  -H "Authorization: $API_KEY"

# Bind the HIGH level to the provider; send {"providerId": null} to clear a binding
curl -s -S -f -X PUT "https://api.docudevs.ai/llm/keys/HIGH" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson providerId "$PROVIDER_ID" '{providerId: $providerId}')"
```

**Python**

```python
# provider_id is the "id" returned when the provider was created or listed.
provider_id = 7

# List the current bindings of DEFAULT, NANO, MINI and HIGH
keys = await client.list_llm_keys()
if keys.status_code != 200:
    raise SystemExit(f"List failed: {keys.status_code}")
print(keys.json())

# Bind the HIGH level to the provider; pass None to clear a binding
response = await client.update_llm_key_binding("HIGH", provider_id)
if response.status_code != 200:
    raise SystemExit(f"Bind failed: {response.status_code}")
```

**CLI**

```bash
# List the current bindings of DEFAULT, NANO, MINI and HIGH
docudevs llm keys

# Bind the HIGH level to provider 7 (the provider "id"); omit --provider-id to clear a binding
docudevs llm bind HIGH --provider-id 7
```

The Python methods `list_llm_keys` and `update_llm_key_binding` return a plain `httpx.Response`, so read the body with `.json()` and check `.status_code`.

## Override reasoning effort for one job

Set the optional `reasoningEffort` request field to override the selected model level's configured effort for the entire job, including pipeline steps and map-reduce chunks. Omit it to keep the built-in model level or your provider's default. Accepted values depend on the selected model and provider; blank values are rejected. See [Reasoning effort](../basics/SimpleDocuments.md#reasoning-effort) for the example.

## Embeddings

Knowledge-base embeddings are configured separately under
**Settings → Embedding Providers** — see
[Bring your own embeddings](https://docs.docudevs.ai/docs/administration/bring-your-own-embeddings).
Chat and embedding bindings are independent: changing an LLM provider never
changes how your knowledge bases are embedded, and vice versa.
