---
title: Bring Your Own OCR
description: Connect your organization's Azure Document Intelligence account for document OCR.
sidebar_position: 5
---
<!-- Generated from docs/administration/bring-your-own-ocr.md. Do not edit by hand. -->

DocuDevs can extract text using your organization's own Azure Document
Intelligence (Azure OCR) account instead of the DocuDevs platform account.

:::note Who can manage OCR providers
Creating, editing, deleting, or key-binding OCR providers is not restricted to
organization admins: any member of the organization and any organization API
key can perform these actions. This differs from
[embedding providers](https://docs.docudevs.ai/docs/administration/bring-your-own-embeddings) and
[model credentials](https://docs.docudevs.ai/docs/administration/model-credentials), which require the
`ADMIN` role for the same operations.
:::

OCR providers do not support shared model credentials: each provider stores
its own `apiKey` directly. This is different from LLM and embedding
providers, which can reference a reusable
[model credential](https://docs.docudevs.ai/docs/administration/model-credentials).

There is no dedicated verify/probe endpoint for OCR providers, unlike
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

**cURL**

```bash
# DOC_INTELLIGENCE_API_KEY holds the key of your Azure Document Intelligence resource.
curl -s -S -f -X POST "https://api.docudevs.ai/ocr/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg apiKey "$DOC_INTELLIGENCE_API_KEY" '{
    name: "Our Doc Intelligence",
    endpoint: "https://example.cognitiveservices.azure.com",
    apiKey: $apiKey,
    modelId: "prebuilt-document"
  }')"
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// DOC_INTELLIGENCE_API_KEY holds the key of your Azure Document Intelligence resource.
JsonNode provider = client.createOcrProvider(
        "Our Doc Intelligence",
        "https://example.cognitiveservices.azure.com",
        System.getenv("DOC_INTELLIGENCE_API_KEY"),
        "prebuilt-document");
System.out.println(provider.toPrettyString());
```

The example reads the Azure Document Intelligence key from the `DOC_INTELLIGENCE_API_KEY` environment variable. The Python SDK's provider methods send field names (not `modelId`) that the API rejects on create. On update the client sends `model`, not `modelId`, so a model change is ignored (`name` and `endpoint` do apply). Use cURL or the Java SDK for these steps. The CLI's create and update commands call the same Python methods.

## Update or delete a provider

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X PATCH "https://api.docudevs.ai/ocr/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"modelId": "prebuilt-invoice"}'
```

**Java**

```java
import ai.docudevs.client.OcrProviderUpdate;
import com.fasterxml.jackson.databind.JsonNode;

// providerId is the "id" returned when the provider was created or listed.
long providerId = 7L;

JsonNode provider = client.updateOcrProvider(
        providerId,
        OcrProviderUpdate.builder().modelId("prebuilt-invoice").build());
System.out.println(provider.toPrettyString());
```

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X DELETE "https://api.docudevs.ai/ocr/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
```

**Python**

```python
# provider_id is the "id" returned when the provider was created or listed.
provider_id = 7

response = await client.delete_ocr_provider(provider_id)
if response.status_code != 204:
    raise SystemExit(f"Delete failed: {response.status_code}")

print(f"HTTP {response.status_code}")
```

**Java**

```java
// providerId is the "id" returned when the provider was created or listed.
long providerId = 7L;

// Returns normally on 204; throws DocuDevsApiException on any other status
client.deleteOcrProvider(providerId);
System.out.println("Deleted provider " + providerId);
```

**CLI**

```bash
# 7 is the provider "id" returned when the provider was created or listed
docudevs ocr delete 7
```

## Key bindings

Each logical OCR key is bound to a provider independently. The only key is `DEFAULT`.

**cURL**

```bash
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

# List the current OCR key bindings
curl -s -S -f "https://api.docudevs.ai/ocr/keys" \
  -H "Authorization: $API_KEY"

# Bind the DEFAULT key to the provider; send {"providerId": null} to clear the binding
curl -s -S -f -X PUT "https://api.docudevs.ai/ocr/keys/DEFAULT" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson providerId "$PROVIDER_ID" '{providerId: $providerId}')"
```

**Python**

```python
# provider_id is the "id" returned when the provider was created or listed.
provider_id = 7

# List the current OCR key bindings
keys = await client.list_ocr_keys()
if keys.status_code != 200:
    raise SystemExit(f"List failed: {keys.status_code}")
print(keys.json())

# Bind the DEFAULT key to the provider; pass None to clear the binding
response = await client.update_ocr_key_binding("DEFAULT", provider_id)
if response.status_code != 200:
    raise SystemExit(f"Bind failed: {response.status_code}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// providerId is the "id" returned when the provider was created or listed.
long providerId = 7L;

// List the current OCR key bindings
JsonNode keys = client.listOcrKeys();
System.out.println(keys.toPrettyString());

// Bind the DEFAULT key to the provider; pass null to clear the binding
client.updateOcrKeyBinding("DEFAULT", providerId);
```

**CLI**

```bash
# List the current OCR key bindings
docudevs ocr keys

# Bind the DEFAULT key to provider 7 (the provider "id"); omit --provider-id to clear the binding
docudevs ocr bind DEFAULT --provider-id 7
```

The Python methods `list_ocr_keys` and `update_ocr_key_binding` return a plain `httpx.Response`, so read the body with `.json()` and check `.status_code`.

## LLM providers

Chat/extraction models are configured separately under
**Settings → LLM Providers** — see
[Bring your own LLM](https://docs.docudevs.ai/docs/administration/bring-your-own-llm). OCR and LLM
bindings are independent of each other.
