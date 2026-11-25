---
title: Bring Your Own Embeddings
description: Connect your organization's Azure OpenAI, OpenAI, or OpenAI-compatible embedding account for knowledge-base search.
sidebar_position: 6
---

DocuDevs indexes knowledge-base documents into Azure AI Search so that
knowledge-base tools can retrieve relevant passages. By default it uses the
DocuDevs platform embedding account (Azure OpenAI `text-embedding-3-large`,
3,072 dimensions). You can instead bind your own Azure OpenAI, OpenAI, or
OpenAI-compatible embedding account for all new work in your organization.
Credentials are managed separately as reusable, organization-scoped model
credentials. A provider revision stores only a `credentialId`; secrets are
write-only and are never returned by the public API.

:::note Admin-only
Creating, editing, deleting, or reindexing an embedding provider requires the
organization `ADMIN` role. Any org member can view providers, the embedding
overview, and case embedding status. An organization API key acts as an
administrator.
:::

## How embedding generations work

Every case has an active *embedding generation*: a specific embedding model
revision (provider, model, dimensions, and similarity metric) with its own
vector field in the shared index. Generations with different dimensions or
metrics coexist in one index and are never compared against each other.

Creating or editing a provider does not reindex existing cases.

- **Credential rotation** through **Settings → Model Credentials** (or
  `POST /model-credentials/{id}/rotate`) never creates a revision and never
  reindexes anything.
- Changing the **endpoint, model, dimensions, similarity metric, or request
  options** creates a new revision. Existing cases keep their current
  generation until you explicitly start a reindex.
- **Binding a revision as the default** only affects *new* case generations.
  Existing cases continue to use their active generation.

## Supported providers, dimensions, and metrics

| Setting | Supported values |
| ------- | ---------------- |
| Provider type | `azure-openai`, `openai`, `openai-compatible` |
| Dimensions | 2 to 4,096 |
| Similarity metric | `cosine`, `dotProduct`, `euclidean` |
| Authentication | `API_KEY` or `ENTRA_CLIENT_SECRET` shared model credential |

For OpenAI, leave the API URL blank. For OpenAI-compatible endpoints, provide
the base URL of the embeddings API.

## Compression behavior and quality tradeoff

Managed generation documents are stored twice in Azure AI Search: the exact
float vector and an `int8` scalar-quantized copy that backs the HNSW
graph. Queries run on the compressed graph and are re-scored against the
preserved original vector, so retrieval quality stays close to the
uncompressed baseline.

## Creating a provider

1. Open **Settings → Model Credentials** and create an `API_KEY` or
   `ENTRA_CLIENT_SECRET` credential. For Entra, provide the tenant ID, client
   ID, and HTTPS `/.default` scope. The secret is accepted only during
   creation or rotation; DocuDevs never returns it.
2. Open **Settings → Embedding Providers** and select **New**.
3. Choose the provider type, enter the endpoint and model, choose the
   dimensions and similarity metric, and select the active shared credential.
4. Save. The revision is validated with a fixed probe before it can be used.

Provider validation starts after save: DocuDevs embeds a fixed probe string
and marks the revision `VALID` or `INVALID`. An `INVALID` revision cannot be
bound as the default or used for reindexing.

Create the shared credential first. This example stores an API key; the key comes from the `PROVIDER_API_KEY` environment variable.

<Example name="model-credentials/create-api-key-credential" />

Then create the provider with the credential's `id`:

<Example name="embedding-providers/create-embedding-provider" />

The create call answers `201 Created` with the provider and its first revision. The generated Python client only parses a `200`, so the Python example reads the body from `response.content`.

For Entra client-secret authentication, create the credential as described in [Model credentials](/docs/administration/model-credentials#create-and-select-a-credential) and pass its `id` the same way. Credential metadata is safe to list, but the secret is never returned.

A shared credential may contain an API key or Microsoft Entra client-secret
credentials. Chat and embedding providers use the same credential object and
rotation flow; see [Model credentials](/docs/administration/model-credentials).
Selecting an Entra credential does not change the embedding generation model,
dimensions, or similarity metric.

## Changing the embedding model

Change the endpoint, model, dimensions, metric, or request options on the
provider. DocuDevs creates a new revision, validates it, and reports
`reindexRequired: true` in the mutation response. Existing cases are not
touched. Changing only `credentialId` reports `CREDENTIAL_ONLY` and does not
reindex or change the vector space. To switch a case after a semantic change:

1. Start a reindex from **Settings → Embedding Providers → Reindex** for the
   case (or the API below).
2. While the candidate generation is being built, the case *dual-writes* to
   both the old and new vector fields, so documents uploaded during the
   reindex appear in both generations and search keeps using the active
   generation with zero downtime.
3. When every document of the candidate generation has indexed successfully
   and the revision is `VALID`, DocuDevs cuts the case over atomically: the
   candidate becomes active and the old generation is retired.

**Cost and storage:** a reindex embeds every document of the case with your
provider again (you are billed for those embeddings), and the case keeps
vectors in both fields until the old generation is cleaned up. Reindexing a
large case can take a while; progress is visible per case.

If the candidate fails (provider outage, quota, dimension mismatch), the
case stays on its active generation, the candidate is marked `FAILED` with a
sanitized error, and you can retry or cancel it. A failed candidate never
blocks other cases.

The case routes below work on organization-wide cases, the ones created with an API key; see [Which cases you can use](../advanced/case-agent.md#which-cases-you-can-use). Read the status of a case's embedding generations:

<Example name="embedding-providers/get-case-embedding-status" />

Start a reindex of the case with the organization default revision. When the reindex has finished, the active generation has the new dimensions and the candidate is gone:

<Example name="embedding-providers/start-case-reindex" />

Retry a failed candidate, or cancel a running reindex:

<Example name="embedding-providers/retry-case-reindex" />

<Example name="embedding-providers/cancel-case-reindex" />

## Default binding

<Example name="embedding-providers/set-default-embedding" />

The default revision is used for new case generations only. The response and
`GET /embeddings/overview` show how many cases run on the default revision,
how many run on older revisions, and how many still use the original
uncompressed index.

## Cases indexed before bring-your-own embeddings

Cases indexed before bring-your-own embeddings are moved automatically to your
organization's default platform revision. You don't need to do anything.
Search keeps working during the move, and a case whose move fails stays
searchable on its current index without affecting other cases.

## Troubleshooting

- **Credential validation succeeds but provider validation fails** — token
  acquisition and the model call are separate checks. Confirm the app role or
  application permission, admin consent, token audience, endpoint, deployment,
  and model settings.
- **Validation fails (`INVALID`)** — the probe embedding call failed. Check
  the endpoint URL, model name, credentials, and that the account can call
  the embeddings API with the configured dimensions. Rotation of the key
  re-validates without creating a revision.
- **Dimension mismatch** — dimensions are fixed per revision. A case
  generated with 3,072 dimensions cannot be searched with a 1,536-dimension
  revision. Start a reindex on the new revision instead.
- **Provider outage during a reindex** — the candidate stays in progress or
  fails; the active generation keeps serving queries. Retry the reindex once
  the provider recovers.
- **Failed candidate** — `GET /cases/{caseId}/embedding` exposes
  `candidate.status` and a sanitized `errorMessage`. Use the retry endpoint
  to start over, or cancel to stop and drop the candidate.
- **Search returns nothing after a reindex** — verify the cutover happened:
  the active generation must have the expected dimensions and the candidate
  must be gone. Documents uploaded *during* the reindex are dual-written and
  appear in both generations.

Usage through your own embedding provider is tracked for observability but,
once the provider or revision is verified (`VALID`), is not charged against
your DocuDevs credit balance. See
[Billing and credits](/docs/reference/billing-credits). LLM providers (chat models) are configured
separately under
[Settings → LLM Providers](/docs/administration/bring-your-own-llm); chat
and embedding bindings are independent of each other.
