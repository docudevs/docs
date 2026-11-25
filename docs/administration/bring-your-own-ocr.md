---
title: Bring Your Own OCR
description: Connect your organization's Azure Document Intelligence account for document OCR.
sidebar_position: 5
---

DocuDevs can extract text using your organization's own Azure Document
Intelligence (Azure OCR) account instead of the DocuDevs platform account.

:::note Who can manage OCR providers
Creating, editing, deleting, or key-binding OCR providers is not restricted to
organization admins: any member of the organization and any organization API
key can perform these actions. This differs from
[embedding providers](/docs/administration/bring-your-own-embeddings) and
[model credentials](/docs/administration/model-credentials), which require the
`ADMIN` role for the same operations.
:::

OCR providers do not support shared model credentials: each provider stores
its own `apiKey` directly. This is different from LLM and embedding
providers, which can reference a reusable
[model credential](/docs/administration/model-credentials).

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

<Example name="ocr-providers/create-ocr-provider" />

The example reads the Azure Document Intelligence key from the `DOC_INTELLIGENCE_API_KEY` environment variable. The Python SDK's provider methods send field names (not `modelId`) that the API rejects on create. On update the client sends `model`, not `modelId`, so a model change is ignored (`name` and `endpoint` do apply). Use cURL for these steps. The CLI's create and update commands call the same methods.

## Update or delete a provider

<Example name="ocr-providers/update-ocr-provider" />

<Example name="ocr-providers/delete-ocr-provider" />

## Key bindings

Each logical OCR key is bound to a provider independently. The only key is `DEFAULT`.

<Example name="ocr-providers/bind-ocr-key" />

The Python methods `list_ocr_keys` and `update_ocr_key_binding` return a plain `httpx.Response`, so read the body with `.json()` and check `.status_code`.

## LLM providers

Chat/extraction models are configured separately under
**Settings → LLM Providers** — see
[Bring your own LLM](/docs/administration/bring-your-own-llm). OCR and LLM
bindings are independent of each other.
