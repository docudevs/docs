---
title: Model Credentials
description: Manage reusable, organization-scoped secrets shared by bring-your-own LLM and embedding providers.
sidebar_position: 7
---
<!-- Generated from docs/administration/model-credentials.md. Do not edit by hand. -->

Model credentials are organization-scoped, write-only secrets that can be
shared by bring-your-own chat and embedding providers. A credential contains
either an API key or Microsoft Entra client-secret authentication. Provider
responses contain only credential metadata; API keys, client secrets, encrypted
values, and brokered access tokens are never returned by the public API.

:::note Admin-only
Only organization administrators can create, rotate, disable, validate, or
delete credentials. Organization members can view the metadata needed to
select a credential. An organization API key acts as an administrator, so the
examples below run with `API_KEY`.
:::

## Microsoft Entra prerequisites

DocuDevs uses the OAuth 2.0 client credentials flow. Your organization must
prepare the app registration; DocuDevs does not create or grant permissions to
it.

1. In Microsoft Entra ID, create or select a single-tenant **daemon/service**
   app registration. It does not need a redirect URI.
2. On the custom AI gateway API registration, expose an application permission
   or app role that permits model calls.
3. Add that **application** permission to the daemon app and grant tenant-wide
   admin consent. A delegated permission is not sufficient because no user is
   present.
4. Create a client secret and copy its **value** immediately. Entra displays the
   value only once; the secret ID is not the client secret.
5. Record the directory (tenant) ID, application (client) ID, secret value, and
   one scope in `{resource}/.default` form, such as
   `api://<gateway-application-id>/.default` or
   `https://gateway.example.com/.default`.

Only the Microsoft Entra Public cloud is supported. One
credential accepts exactly one `.default` scope. Configure the least-privilege
app role on the gateway and grant no unrelated Microsoft Graph permissions.

## Create and select a credential

Open **Settings → Model Credentials**, select **New credential**, and choose
**Entra client secret**. Enter the Entra values and, if known, the secret expiry
date. The expiry date is metadata for operators: DocuDevs does not renew the
secret. Rotate it before Entra expires it.

**cURL**

```bash
# ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$ENTRA_CLIENT_SECRET" '{
    name: "Enterprise AI gateway",
    description: "Shared by chat and embeddings",
    authType: "ENTRA_CLIENT_SECRET",
    tenantId: "00000000-0000-0000-0000-000000000000",
    clientId: "11111111-1111-1111-1111-111111111111",
    secret: $secret,
    scope: "api://22222222-2222-2222-2222-222222222222/.default",
    cloud: "PUBLIC",
    secretExpiresAt: "2027-08-25T00:00:00Z"
  }')"
```

**Python**

```python
# ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
response = await client.create_model_credential(
    name="Enterprise AI gateway",
    description="Shared by chat and embeddings",
    auth_type="ENTRA_CLIENT_SECRET",
    tenant_id="00000000-0000-0000-0000-000000000000",
    client_id="11111111-1111-1111-1111-111111111111",
    secret=os.environ["ENTRA_CLIENT_SECRET"],
    scope="api://22222222-2222-2222-2222-222222222222/.default",
    cloud="PUBLIC",
    secret_expires_at="2027-08-25T00:00:00Z",
)
if response.status_code not in (200, 201):
    raise SystemExit(f"Create failed: {response.status_code} {response.content.decode()}")

credential = json.loads(response.content)
print(f"Credential {credential['id']}: {credential['status']}")
```

**Java**

```java
import ai.docudevs.client.ModelCredentialOptions;
import com.fasterxml.jackson.databind.JsonNode;

// ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
// Java: the call returns the JSON body of the 201 response and throws DocuDevsApiException on an error status.
JsonNode credential = client.createModelCredential(
        "Enterprise AI gateway",
        System.getenv("ENTRA_CLIENT_SECRET"),
        ModelCredentialOptions.builder()
                .authType("ENTRA_CLIENT_SECRET")
                .description("Shared by chat and embeddings")
                .tenantId("00000000-0000-0000-0000-000000000000")
                .clientId("11111111-1111-1111-1111-111111111111")
                .scope("api://22222222-2222-2222-2222-222222222222/.default")
                .cloud("PUBLIC")
                .secretExpiresAt("2027-08-25T00:00:00Z")
                .build());
System.out.println("Credential " + credential.path("id").asLong() + ": " + credential.path("status").asText());
```

The secret goes in the write-only `secret` field; the
example reads it from the `ENTRA_CLIENT_SECRET` environment variable. For an
API-key credential, set `authType` to `API_KEY` and send the key as `secret`:

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
// Java: the call returns the JSON body of the 201 response and throws DocuDevsApiException on an error status.
JsonNode credential = client.createModelCredential("Provider embeddings", System.getenv("PROVIDER_API_KEY"));
System.out.println("Credential " + credential.path("id").asLong() + ": " + credential.path("status").asText());
```

The create call answers `201 Created` with the credential metadata, including its `id`. In Python, read
the body from `response.content`: the generated Python client only parses a `200` into `response.parsed`.

Select the credential when creating or updating either an LLM provider or an
embedding provider. A credential may be shared by both. The provider response
contains `credentialId` and a metadata summary, never secret material.

Inline API keys on a provider remain accepted. Prefer a shared credential when the same enterprise gateway identity is reused.

## List, get, and update credentials

List the credentials of the organization, read one, or change its metadata. Members of the organization can list and read credentials; updating needs an administrator.

**cURL**

```bash
curl -s -S -f "https://api.docudevs.ai/model-credentials" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
response = await client.list_model_credentials()
if response.status_code != 200:
    raise SystemExit(f"List failed: {response.status_code}")

for credential in response.parsed or []:
    print(f"{credential.id}: {credential.name} ({credential.status})")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode credentials = client.listModelCredentials();
for (JsonNode credential : credentials) {
    System.out.println(credential.path("id").asLong() + ": " + credential.path("name").asText()
            + " (" + credential.path("status").asText() + ")");
}
```

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

curl -s -S -f "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY"
```

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

curl -s -S -f -X PATCH "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"description": "Updated description"}'
```

The Python and Java SDKs list credentials, but have no method for reading one credential
or updating its metadata, so the last two examples are cURL only.

## Validate the credential and provider

These checks answer different questions:

- `POST /model-credentials/{id}/validate` confirms that an API key can be
  decrypted or that DocuDevs can acquire an Entra JWT for the configured
  tenant, client, secret, and scope.
- `POST /llm/providers/{id}/verify` calls the configured chat model endpoint.
  Embedding providers run their fixed probe embedding when the revision is
  created or changed.

A valid token does not prove that the gateway accepts its audience or roles, or
that the endpoint, deployment, model, and dimensions are correct. Run both
levels of validation. Errors exposed by DocuDevs are sanitized and do not
include Entra response bodies, client secrets, API keys, or access tokens.

At request time DocuDevs acquires and caches the Entra access token, refreshes
it before expiry, and sends it to the custom gateway as a bearer token. If the
credential is disabled, expired or invalid, token acquisition fails, or the
gateway rejects the token, the affected call fails closed. It is never silently
routed to a DocuDevs platform model or another credential.

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/validate" \
  -H "Authorization: $API_KEY"
```

Neither the Python SDK nor the Java SDK has a method for validating a credential.

## Rotate, disable, enable, and delete

Rotate a credential from **Settings → Model Credentials**, or call the rotate endpoint with the new secret only:

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
# NEW_PROVIDER_API_KEY holds the replacement secret.
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/rotate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$NEW_PROVIDER_API_KEY" '{secret: $secret}')"
```

**Python**

```python
# credential_id is the "id" returned when the credential was created or listed.
# NEW_PROVIDER_API_KEY holds the replacement secret.
credential_id = 42

response = await client.rotate_model_credential(
    credential_id, os.environ["NEW_PROVIDER_API_KEY"]
)
if response.status_code != 200:
    raise SystemExit(f"Rotate failed: {response.status_code}")

print(f"Credential version: {response.parsed.credential_version}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// credentialId is the "id" returned when the credential was created or listed.
// NEW_PROVIDER_API_KEY holds the replacement secret.
long credentialId = 42L;

JsonNode rotated = client.rotateModelCredential(credentialId, System.getenv("NEW_PROVIDER_API_KEY"));
System.out.println("Credential version: " + rotated.path("credentialVersion").asLong());
```

Rotation increments `credentialVersion`, invalidates cached tokens, and marks
credential and provider validation stale. All attached chat and embedding
providers use the new secret on their next call. Rotation does not change an embedding revision or start a reindex.

Disabling a credential immediately blocks resolution for every attached
provider. Re-enable and validate it to restore use. A referenced credential
cannot be deleted; detach it from all chat and embedding providers first.

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

# Block every provider that uses the credential
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/disable" \
  -H "Authorization: $API_KEY"

# Allow them again
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/enable" \
  -H "Authorization: $API_KEY"
```

`PATCH /model-credentials/{id}` with `{"status": "DISABLED"}` is the equivalent
generic form.

**cURL**

```bash
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

# 204 when deleted, 409 while a provider still references the credential
curl -s -S -X DELETE "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
```

**Python**

```python
# credential_id is the "id" returned when the credential was created or listed.
credential_id = 42

# 204 when deleted, 409 while a provider still references the credential
response = await client.delete_model_credential(credential_id)
print(f"HTTP {response.status_code.value}")
```

**Java**

```java
// credentialId is the "id" returned when the credential was created or listed.
long credentialId = 42L;

// Returns normally on 204 when deleted; throws DocuDevsApiException with status 409 while a provider still references the credential
client.deleteModelCredential(credentialId);
System.out.println("Deleted credential " + credentialId);
```

Neither the Python SDK nor the Java SDK has methods for disabling or enabling a credential, so the
first example is cURL only.
