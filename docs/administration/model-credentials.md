---
title: Model Credentials
description: Manage reusable, organization-scoped secrets shared by bring-your-own LLM and embedding providers.
sidebar_position: 7
---

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

<Example name="model-credentials/create-entra-credential" />

The secret goes in the write-only `secret` field; the
example reads it from the `ENTRA_CLIENT_SECRET` environment variable. For an
API-key credential, set `authType` to `API_KEY` and send the key as `secret`:

<Example name="model-credentials/create-api-key-credential" />

The create call answers `201 Created` with the credential metadata, including its `id`. In Python, read
the body from `response.content`: the generated Python client only parses a `200` into `response.parsed`.

Select the credential when creating or updating either an LLM provider or an
embedding provider. A credential may be shared by both. The provider response
contains `credentialId` and a metadata summary, never secret material.

Inline API keys on a provider remain accepted. Prefer a shared credential when the same enterprise gateway identity is reused.

## List, get, and update credentials

List the credentials of the organization, read one, or change its metadata. Members of the organization can list and read credentials; updating needs an administrator.

<Example name="model-credentials/list-credentials" />

<Example name="model-credentials/get-credential" />

<Example name="model-credentials/update-credential" />

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

<Example name="model-credentials/validate-credential" />

Neither the Python SDK nor the Java SDK has a method for validating a credential.

## Rotate, disable, enable, and delete

Rotate a credential from **Settings → Model Credentials**, or call the rotate endpoint with the new secret only:

<Example name="model-credentials/rotate-credential" />

Rotation increments `credentialVersion`, invalidates cached tokens, and marks
credential and provider validation stale. All attached chat and embedding
providers use the new secret on their next call. Rotation does not change an embedding revision or start a reindex.

Disabling a credential immediately blocks resolution for every attached
provider. Re-enable and validate it to restore use. A referenced credential
cannot be deleted; detach it from all chat and embedding providers first.

<Example name="model-credentials/disable-and-enable-credential" />

`PATCH /model-credentials/{id}` with `{"status": "DISABLED"}` is the equivalent
generic form.

<Example name="model-credentials/delete-credential" />

Neither the Python SDK nor the Java SDK has methods for disabling or enabling a credential, so the
first example is cURL only.
