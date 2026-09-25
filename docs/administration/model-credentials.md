---
title: Model Credentials
description: Manage reusable, organization-scoped secrets shared by bring-your-own LLM and embedding providers.
sidebar_position: 7
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Model credentials are organization-scoped, write-only secrets that can be
shared by bring-your-own chat and embedding providers. A credential contains
either an API key or Microsoft Entra client-secret authentication. Provider
responses contain only credential metadata; API keys, client secrets, encrypted
values, and brokered access tokens are never returned by the public API.

:::note Admin-only
Only organization administrators can create, rotate, disable, validate, or
delete credentials. Organization members can view the metadata needed to
select a credential.
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

The initial release supports the Microsoft Entra Public cloud only. One
credential accepts exactly one `.default` scope. Configure the least-privilege
app role on the gateway and grant no unrelated Microsoft Graph permissions.

## Create and select a credential

Open **Settings → Model Credentials**, select **New credential**, and choose
**Entra client secret**. Enter the Entra values and, if known, the secret expiry
date. The expiry date is metadata for operators: DocuDevs does not renew the
secret. Rotate it before Entra expires it.

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

curl -X POST "$API_URL/model-credentials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise AI gateway",
    "description": "Shared by chat and embeddings",
    "authType": "ENTRA_CLIENT_SECRET",
    "tenantId": "00000000-0000-0000-0000-000000000000",
    "clientId": "11111111-1111-1111-1111-111111111111",
    "clientSecret": "<secret-value>",
    "scope": "api://22222222-2222-2222-2222-222222222222/.default",
    "cloud": "PUBLIC",
    "secretExpiresAt": "2027-08-25T00:00:00Z"
  }'
```

  </TabItem>
  <TabItem value="python">

`DocuDevsClient.create_model_credential` wraps the generic `secret` field, so
use it for API-key credentials. For an Entra client-secret credential, call
the generated function directly so you can pass `tenant_id`, `client_id`,
`scope`, `cloud`, and `client_secret`:

```python
from docudevs import DocuDevsClient

client = DocuDevsClient(api_url="https://api.docudevs.ai", token="<your-api-key>")

# API-key credential via the convenience wrapper
credential = await client.create_model_credential(
    name="OpenAI embeddings",
    secret="sk-...",
    auth_type="API_KEY",
)
credential_id = credential.parsed.id

# Entra client-secret credential via the generated typed function
from docudevs.api.model_credentials.create_model_credential import asyncio_detailed as create_model_credential
from docudevs.client import AuthenticatedClient
from docudevs.models.create_model_credential_request import CreateModelCredentialRequest

api_client = AuthenticatedClient(base_url="https://api.docudevs.ai", token="<your-api-key>")

body = CreateModelCredentialRequest(
    name="Enterprise AI gateway",
    description="Shared by chat and embeddings",
    auth_type="ENTRA_CLIENT_SECRET",
    tenant_id="00000000-0000-0000-0000-000000000000",
    client_id="11111111-1111-1111-1111-111111111111",
    client_secret="<secret-value>",
    scope="api://22222222-2222-2222-2222-222222222222/.default",
    cloud="PUBLIC",
)
response = await create_model_credential(client=api_client, body=body)
entra_credential = response.parsed
```

`create_model_credential` (and the other convenience wrappers below) return
the generated client's `Response[...]` object, so read fields off `.parsed`
(snake_case attributes), not by treating the result as a dict. Generated
functions that `DocuDevsClient` does not wrap take an `AuthenticatedClient`
(`api_client` above) rather than the `DocuDevsClient` instance itself.

  </TabItem>
</Tabs>

Select the credential when creating or updating either an LLM provider or an
embedding provider. A credential may be shared by both. The provider response
contains `credentialId` and a metadata summary, never secret material.

For compatibility, inline API keys remain accepted. New configurations should
prefer a shared credential when the same enterprise gateway identity is reused.

## List, get, and update credentials

`list_model_credentials` and `create_model_credential` are the only
convenience wrappers `DocuDevsClient` provides for this resource today. `get`,
`update`, `enable`, `disable`, and `validate` are only available as generated
typed functions or plain REST calls:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl "$API_URL/model-credentials" -H "Authorization: $API_KEY"

curl "$API_URL/model-credentials/42" -H "Authorization: $API_KEY"

curl -X PATCH "$API_URL/model-credentials/42" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"description": "Updated description"}'
```

  </TabItem>
  <TabItem value="python">

```python
credentials = await client.list_model_credentials()
metadata = credentials.parsed  # list[ModelCredentialDto]

from docudevs.api.model_credentials.get_model_credential import asyncio_detailed as get_model_credential
from docudevs.api.model_credentials.update_model_credential import asyncio_detailed as update_model_credential
from docudevs.models.update_model_credential_request import UpdateModelCredentialRequest

one = await get_model_credential(id=42, client=api_client)

body = UpdateModelCredentialRequest(description="Updated description")
updated = await update_model_credential(id=42, client=api_client, body=body)
```

  </TabItem>
</Tabs>

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

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl -X POST "$API_URL/model-credentials/42/validate" -H "Authorization: $API_KEY"
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.api.model_credentials.validate_model_credential import asyncio_detailed as validate_model_credential

result = await validate_model_credential(id=42, client=api_client)
```

  </TabItem>
</Tabs>

## Rotate, disable, enable, and delete

Rotate a credential from **Settings → Model Credentials**, or update only its
secret field. `rotate_model_credential` is the one write operation that is
both wrapped by `DocuDevsClient` and has a dedicated REST endpoint:

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl -X POST "$API_URL/model-credentials/42/rotate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"secret": "sk-new-...", "secretExpiresAt": "2028-08-25T00:00:00Z"}'
```

  </TabItem>
  <TabItem value="python">

```python
rotated = await client.rotate_model_credential(42, "sk-new-...")
rotated_credential = rotated.parsed
```

  </TabItem>
</Tabs>

Rotation increments `credentialVersion`, invalidates cached tokens, and marks
credential and provider validation stale. All attached chat and embedding
providers use the new secret on their next call. API-key rotation is propagated
to the legacy compatibility columns in the same operation. Neither kind of
rotation changes an embedding revision or starts a reindex.

Disabling a credential immediately blocks resolution for every attached
provider. Re-enable and validate it to restore use. A referenced credential
cannot be deleted; detach it from all chat and embedding providers first.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">

```bash
curl -X POST "$API_URL/model-credentials/42/disable" -H "Authorization: $API_KEY"
curl -X POST "$API_URL/model-credentials/42/enable" -H "Authorization: $API_KEY"

# equivalent generic form:
curl -X PATCH "$API_URL/model-credentials/42" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"status": "DISABLED"}'

curl -X DELETE "$API_URL/model-credentials/42" -H "Authorization: $API_KEY"
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.api.model_credentials.disable_model_credential import asyncio_detailed as disable_model_credential
from docudevs.api.model_credentials.enable_model_credential import asyncio_detailed as enable_model_credential

await disable_model_credential(id=42, client=api_client)
await enable_model_credential(id=42, client=api_client)

await client.delete_model_credential(42)
```

  </TabItem>
</Tabs>

## Rollout and rollback

Operators enable Entra attachment only after the API token broker and all chat
and embedding workers support the typed authorization contract. Until both
readiness gates are enabled, attempts to attach an Entra credential are
rejected; existing API-key providers continue to work.

Database changes are additive so an older API version can continue running
against the upgraded schema during a rolling deployment. API-key credentials
also retain their legacy provider values for old workers. Entra credentials do
not place their client secret in those legacy columns.

Before rolling back to workers that understand only API keys, disable or detach
every Entra-backed chat and embedding binding and wait for in-flight work to
finish. Old workers cannot use an Entra binding, and rollback must never replace
the broker sentinel with the client secret. Re-enable Entra bindings only after
all typed-authorization workers are healthy again.
