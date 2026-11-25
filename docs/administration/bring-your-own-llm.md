---
title: Bring Your Own LLM
description: Connect your organization's Azure OpenAI, OpenAI, or OpenAI-compatible account for chat/extraction models.
sidebar_position: 4
---

DocuDevs can process documents using your organization's Azure OpenAI,
OpenAI, or OpenAI-compatible account. Azure OpenAI and OpenAI are fully
supported. OpenAI-compatible endpoints are supported best-effort through
the Chat Completions API.

:::note Who can manage LLM providers
Creating, editing, deleting, verifying, or key-binding LLM providers is not
restricted to organization admins: any member of the organization and any
organization API key can perform these actions. This differs from
[embedding providers](/docs/administration/bring-your-own-embeddings) and
[model credentials](/docs/administration/model-credentials), which require the
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
6. Select a shared model credential (not for the OpenAI type, whose form sends
   no API URL; create that provider through the API), or enter an API key to
   create a dedicated credential, and save the provider.
7. Bind the provider to one or more levels: `DEFAULT`, `NANO`, `MINI`, or
   `HIGH`.

Only explicitly bound levels use the provider. Unbound levels continue to use
DocuDevs platform models.

## Create a provider

Create a provider that uses a shared [model credential](/docs/administration/model-credentials). `modelType` is `azure-openai`, `openai` or `openai-compatible`. `deploymentName` is required (the model name for `openai`), and a provider that uses a credential needs an `https` `apiUrl`.

<Example name="llm-providers/create-llm-provider" />

The Python SDK's provider methods send field names the API rejects on create and ignores on update, so use cURL for these steps. The CLI's create and update commands call the same methods.

## Update or delete a provider

<Example name="llm-providers/update-llm-provider" />

<Example name="llm-providers/delete-llm-provider" />

## Verify credentials

Run a document-processing job after binding the provider. The real job is the
credential check and exercises the same structured-output, vision, streaming,
and usage path as production work. Invalid credentials or configuration fail
the affected job; DocuDevs does not silently reroute it to a platform model.

You can also probe a provider directly with **Verify provider**, which calls
the configured chat endpoint and checks that the token, gateway
authorization, deployment, and model settings work together:

<Example name="llm-providers/verify-llm-provider" />

The Python SDK has no method for verifying a provider, so this example is cURL only.

Shared credentials can use either an API key or a Microsoft Entra client
ID/client secret. See [Model credentials](/docs/administration/model-credentials)
for the Entra app-registration prerequisites, rotation, and validation. The same shared credential can be selected by chat and
embedding providers.

Provider secrets are write-only in the public API and are not returned to the UI.
Usage through your own provider is tracked for observability but is not charged
against your DocuDevs credit balance. The per-job fee still applies; see
[Billing and credits](/docs/reference/billing-credits).

For a shared credential, **Validate credential** proves that DocuDevs can decrypt
an API key or acquire an Entra access token. It does not call the model gateway.
Use **Verify provider** to call the configured chat endpoint and verify that the
token, gateway authorization, deployment, and model settings work together.
Either check fails closed; DocuDevs does not fall back to a platform model.

## Key bindings

Each logical level (`DEFAULT`, `NANO`, `MINI`, `HIGH`) is bound to a provider
independently. Binding a level to a provider, or clearing it, takes the provider `id`.

<Example name="llm-providers/bind-llm-level" />

The Python methods `list_llm_keys` and `update_llm_key_binding` return a plain `httpx.Response`, so read the body with `.json()` and check `.status_code`.

## Override reasoning effort for one job

Set the optional `reasoningEffort` request field to override the selected model level's configured effort for the entire job, including pipeline steps and map-reduce chunks. Omit it to keep the built-in model level or your provider's default. Accepted values depend on the selected model and provider; blank values are rejected. See [Reasoning effort](../basics/SimpleDocuments.md#reasoning-effort) for the example.

## Embeddings

Knowledge-base embeddings are configured separately under
**Settings → Embedding Providers** — see
[Bring your own embeddings](/docs/administration/bring-your-own-embeddings).
Chat and embedding bindings are independent: changing an LLM provider never
changes how your knowledge bases are embedded, and vice versa.
