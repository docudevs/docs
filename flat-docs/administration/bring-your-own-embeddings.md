---
title: Bring Your Own Embeddings
description: Connect your organization's Azure OpenAI, OpenAI, or OpenAI-compatible embedding account for knowledge-base search.
sidebar_position: 6
---
<!-- Generated from docs/administration/bring-your-own-embeddings.md. Do not edit by hand. -->

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

**cURL**

```bash
# PROVIDER_API_KEY holds the API key issued by your model provider.
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$PROVIDER_API_KEY" '{
    name: "Provider embeddings",
    authType: "API_KEY",
    secret: $secret
  }')"
```

**Python**

```python
# PROVIDER_API_KEY holds the API key issued by your model provider.
response = await client.create_model_credential(
    name="Provider embeddings",
    auth_type="API_KEY",
    secret=os.environ["PROVIDER_API_KEY"],
)
if response.status_code not in (200, 201):
    raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

credential = json.loads(response.content)
print(f"Credential {credential['id']}: {credential['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// PROVIDER_API_KEY holds the API key issued by your model provider.
// The call returns the JSON body of the 201 response and throws DocuDevsApiException on an error status.
JsonNode credential = client.createModelCredential("Provider embeddings", System.getenv("PROVIDER_API_KEY"));
System.out.println("Credential " + credential.path("id").asLong() + ": " + credential.path("status").asText());
```

Then create the provider with the credential's `id`:

**cURL**

```bash
# CREDENTIAL_ID is the "id" of a shared model credential (see Model credentials).
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/embeddings/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson credentialId "$CREDENTIAL_ID" '{
    name: "Our OpenAI embeddings",
    providerType: "openai",
    apiUrl: "",
    deploymentName: "text-embedding-3-small",
    dimensions: 1536,
    similarityMetric: "cosine",
    requestOptions: {},
    credentialId: $credentialId,
    setAsDefault: false
  }')"
```

**Python**

```python
# credential_id is the "id" of a shared model credential (see Model credentials).
credential_id = 42

response = await client.create_embedding_provider(
    name="Our OpenAI embeddings",
    provider_type="openai",
    deployment_name="text-embedding-3-small",
    dimensions=1536,
    similarity_metric="cosine",
    credential_id=credential_id,
)
if response.status_code not in (200, 201):
    raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

mutation = json.loads(response.content)
revision = mutation["provider"]["latestRevision"]
print(f"Revision {revision['id']}, reindex required: {mutation['reindexRequired']}")
```

**Java**

```java
import ai.docudevs.client.EmbeddingProviderOptions;
import com.fasterxml.jackson.databind.JsonNode;

// credentialId is the "id" of a shared model credential (see Model credentials).
long credentialId = 42L;

JsonNode mutation = client.createEmbeddingProvider(
        "Our OpenAI embeddings",
        "openai",
        "text-embedding-3-small",
        1536,
        EmbeddingProviderOptions.builder()
                .apiUrl("")
                .similarityMetric("cosine")
                .requestOptions(Map.of())
                .credentialId(credentialId)
                .setAsDefault(false)
                .build());
JsonNode revision = mutation.path("provider").path("latestRevision");
System.out.println("Revision " + revision.path("id").asLong()
        + ", reindex required: " + mutation.path("reindexRequired").asBoolean());
```

The create call answers `201 Created` with the provider and its first revision. The generated Python client only parses a `200`, so the Python example reads the body from `response.content`.

For Entra client-secret authentication, create the credential as described in [Model credentials](https://docs.docudevs.ai/docs/administration/model-credentials#create-and-select-a-credential) and pass its `id` the same way. Credential metadata is safe to list, but the secret is never returned.

A shared credential may contain an API key or Microsoft Entra client-secret
credentials. Chat and embedding providers use the same credential object and
rotation flow; see [Model credentials](https://docs.docudevs.ai/docs/administration/model-credentials).
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

**cURL**

```bash
# CASE_ID is the id of an organization-wide case.
CASE_ID=3

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/embedding" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
# case_id is the id of an organization-wide case.
case_id = 3

response = await client.get_case_embedding_status(case_id)
if response.status_code != 200:
    raise SystemExit(f"Status failed: {response.status_code}")

status = json.loads(response.content)
active = status.get("active")
candidate = status.get("candidate")
print(f"Active dimensions: {active['dimensions'] if active else None}")
print(f"Candidate status: {candidate['status'] if candidate else None}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// caseId is the id of an organization-wide case.
long caseId = 3L;

JsonNode status = client.getCaseEmbeddingStatus(caseId);
JsonNode active = status.path("active");
JsonNode candidate = status.path("candidate");
System.out.println("Active dimensions: " + (active.isObject() ? active.path("dimensions").asText() : null));
System.out.println("Candidate status: " + (candidate.isObject() ? candidate.path("status").asText() : null));
```

Start a reindex of the case with the organization default revision. When the reindex has finished, the active generation has the new dimensions and the candidate is gone:

**cURL**

```bash
# CASE_ID is the id of an organization-wide case.
CASE_ID=3

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/embedding-reindex" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
# case_id is the id of an organization-wide case.
case_id = 3

response = await client.start_case_embedding_reindex(case_id)
if response.status_code != 200:
    raise SystemExit(f"Reindex failed: {response.status_code} {response.content.decode()}")

candidate = json.loads(response.content)
print(f"Candidate generation {candidate['id']}: {candidate['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// caseId is the id of an organization-wide case.
long caseId = 3L;

JsonNode candidate = client.startCaseEmbeddingReindex(caseId);
System.out.println("Candidate generation " + candidate.path("id").asText() + ": " + candidate.path("status").asText());
```

Retry a failed candidate, or cancel a running reindex:

**cURL**

```bash
# CASE_ID is the id of an organization-wide case.
CASE_ID=3

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/embedding-reindex/retry" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
# case_id is the id of an organization-wide case.
case_id = 3

response = await client.retry_case_embedding_reindex(case_id)
if response.status_code != 200:
    raise SystemExit(f"Retry failed: {response.status_code} {response.content.decode()}")

candidate = json.loads(response.content)
print(f"Candidate generation {candidate['id']}: {candidate['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// caseId is the id of an organization-wide case.
long caseId = 3L;

JsonNode candidate = client.retryCaseEmbeddingReindex(caseId);
System.out.println("Candidate generation " + candidate.path("id").asText() + ": " + candidate.path("status").asText());
```

**cURL**

```bash
# CASE_ID is the id of an organization-wide case.
CASE_ID=3

curl -s -S -f -X DELETE "https://api.docudevs.ai/cases/$CASE_ID/embedding-reindex" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
# case_id is the id of an organization-wide case.
case_id = 3

response = await client.cancel_case_embedding_reindex(case_id)
if response.status_code != 200:
    raise SystemExit(f"Cancel failed: {response.status_code} {response.content.decode()}")

candidate = json.loads(response.content)
print(f"Candidate generation {candidate['id']}: {candidate['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// caseId is the id of an organization-wide case.
long caseId = 3L;

JsonNode candidate = client.cancelCaseEmbeddingReindex(caseId);
System.out.println("Candidate generation " + candidate.path("id").asText() + ": " + candidate.path("status").asText());
```

## Default binding

**cURL**

```bash
# REVISION_ID is the revision "id" under provider.latestRevision when the provider was created.
REVISION_ID=12

curl -s -S -f -X PUT "https://api.docudevs.ai/embeddings/default" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson revisionId "$REVISION_ID" '{revisionId: $revisionId}')"

# Counts of cases on the default revision, on older revisions and on the original index
curl -s -S -f "https://api.docudevs.ai/embeddings/overview" \
  -H "Authorization: $API_KEY" | jq .defaultBinding
```

**Python**

```python
# revision_id is the revision "id" under provider.latestRevision when the provider was created.
revision_id = 12

response = await client.set_default_embedding_binding(revision_id)
if response.status_code != 200:
    raise SystemExit(f"Bind failed: {response.status_code} {response.content.decode()}")

# Counts of cases on the default revision, on older revisions and on the original index
overview = await client.get_embedding_overview()
if overview.status_code != 200:
    raise SystemExit(f"Overview failed: {overview.status_code}")
print(json.dumps(json.loads(overview.content)["defaultBinding"], indent=2))
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// revisionId is the revision "id" under provider.latestRevision when the provider was created.
long revisionId = 12L;

client.setDefaultEmbeddingBinding(revisionId);

// Counts of cases on the default revision, on older revisions and on the original index
JsonNode overview = client.getEmbeddingOverview();
System.out.println(overview.path("defaultBinding").toPrettyString());
```

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
[Billing and credits](https://docs.docudevs.ai/docs/reference/billing-credits). LLM providers (chat models) are configured
separately under
[Settings → LLM Providers](https://docs.docudevs.ai/docs/administration/bring-your-own-llm); chat
and embedding bindings are independent of each other.
