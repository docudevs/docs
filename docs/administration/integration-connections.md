---
title: Integration Connections
description: Store reusable, org-scoped credentials for flow-builder connector nodes.
---

# Integration Connections

An integration connection is a named, organization-scoped bundle of credentials for one connector family — for example a PostgreSQL JDBC URL, username, and password, or an Azure Blob Storage connection string. Create a connection once and reference it by name from any flow-builder node instead of pasting credentials into every flow.

## Where to manage connections

Connections live at **Connections** in the sidebar's **Integrations** section (`/app/integrations/connections`). This page — and the underlying `POST`/`PUT`/`DELETE` calls — is restricted to the `ADMIN` role; `MEMBER` accounts can view connections but cannot create, edit, or delete them.

## Connector families

The fields a connection accepts depend on its `connectorFamily`:

| Family | Covers |
| --- | --- |
| `microsoft365` | SharePoint, Outlook |
| `aws` | Amazon S3 |
| `gcp` | Google Cloud Storage, Google Drive, Google Sheets |
| `azure` | Azure Blob Storage |
| `postgres` | PostgreSQL |
| `mysql` | MySQL |
| `sqlserver` | SQL Server |

## Write-only secrets

Fields marked as secret (for example a database password or an API key) are write-only: `GET`/`POST`/`PUT` responses never return their values, only which field names are currently set as secrets (`secretFieldsSet`). Send secret values in the `secrets` map on create or update; on update, any secret field you omit keeps its previously stored value — you only need to send the fields you're changing.

## Referencing a connection in a flow

In the flow builder, a connector node's fields can reference a connection's fields with `{{ connection('name').field }}`, where `name` is the connection's `name` and `field` is one of its configured field keys (for example `{{ connection('warehouse-pg').password }}`). The reference is resolved when the flow dispatches, not when it's saved.

## Deleting a connection that's in use

Deleting a connection referenced by a saved flow configuration is blocked with an HTTP 409 response, listing which flow configurations reference it:

```json
{
  "message": "Connection 'warehouse-pg' is referenced by 1 flow config(s) and cannot be deleted.",
  "connectionName": "warehouse-pg",
  "referencedBy": [{ "id": 12, "flowId": "sync-invoices" }]
}
```

Remove or update the referencing flow configuration(s) before deleting the connection.

## REST reference

| Method | Path | Role | Notes |
| --- | --- | --- | --- |
| GET | `/integration/connections` | ADMIN, MEMBER | List connections for the organization |
| POST | `/integration/connections` | ADMIN | Create — `{name, connectorFamily, fieldValues, secrets, description?, active}` |
| GET | `/integration/connections/{id}` | ADMIN, MEMBER | Get a connection |
| PUT | `/integration/connections/{id}` | ADMIN | Update — `{fieldValues, secrets, description?, active?}` |
| DELETE | `/integration/connections/{id}` | ADMIN | Delete — 409 if referenced by a flow configuration |

## Examples

```bash
curl -X POST https://api.docudevs.ai/integration/connections \
  -H "Authorization: $API_KEY" -H "Content-Type: application/json" \
  -d '{"name":"warehouse-pg","connectorFamily":"postgres","active":true,
       "fieldValues":{"jdbcUrl":"jdbc:postgresql://host:5432/db","username":"svc"},
       "secrets":{"password":"s3cret"}}'
```

```bash
curl -X DELETE https://api.docudevs.ai/integration/connections/7 \
  -H "Authorization: $API_KEY"
# 409 Conflict if referenced by a flow config
```

```bash
curl https://api.docudevs.ai/integration/connections -H "Authorization: $API_KEY"
```
