---
title: Set up integration connections
slug: /guides/integration-connections
---

# Set up integration connections

An organization admin creates a managed connection under **Integrations → Connections → New connection**. Choose the system, give the connection a name, and enter the credentials from that system. Select the saved connection on the corresponding node in **Flow Builder**. Supply any remaining targets on the node. The runtime must be able to reach the provider or database.

## SharePoint

### Register and authorize the application

1. In **Microsoft Entra admin center → App registrations**, register an application in the tenant that owns the SharePoint site. Copy the **Directory (tenant) ID** and **Application (client) ID** from **Overview**. [Register a Microsoft Entra application](https://learn.microsoft.com/en-us/graph/auth-register-app-v2)
2. Under **Certificates & secrets → Client secrets**, create a client secret. Copy its **Value** while it is visible. [Create a client secret](https://learn.microsoft.com/en-us/entra/msidweb/authentication/client-secrets)
3. Under **API permissions → Add a permission → Microsoft Graph → Application permissions**, add `Sites.Selected` and have a tenant admin grant consent. Have a SharePoint administrator grant the app access to the specific site. Use `read` for list and download, or `write` for upload. A separate admin identity makes this grant. [Selected permissions](https://learn.microsoft.com/en-us/graph/permissions-selected-overview) · [Grant an app access to a site](https://learn.microsoft.com/en-us/graph/api/site-post-permissions?view=graph-rest-1.0)

### Register the app with Azure CLI

As an alternative to registering the app in the portal, run the following commands in Bash. Replace the tenant ID with the ID of the tenant that owns the SharePoint site. A Global Administrator in that tenant runs the `admin-consent` command. Run each command successfully before moving to the next one. [Azure CLI app registration](https://learn.microsoft.com/en-us/cli/azure/ad/app) · [Grant admin consent with Azure CLI](https://learn.microsoft.com/en-us/cli/azure/ad/app/permission#az-ad-app-permission-admin-consent)

```bash
TENANT_ID="your-entra-tenant-id"
az login --tenant "$TENANT_ID" --allow-no-subscriptions --output none

CLIENT_ID=$(az ad app create \
  --display-name "DocuDevs SharePoint" \
  --sign-in-audience AzureADMyOrg \
  --query appId --output tsv)

az ad sp create --id "$CLIENT_ID" --output none

# Microsoft Graph application permission Sites.Selected
az ad app permission add --id "$CLIENT_ID" \
  --api 00000003-0000-0000-c000-000000000000 \
  --api-permissions 883ea226-0bf2-4a8f-9f9d-92c9162a727d=Role

az ad app permission admin-consent --id "$CLIENT_ID"

printf 'Tenant ID: %s\nClient ID: %s\n' "$TENANT_ID" "$CLIENT_ID"
```

Run the next command in a private terminal. It prints the new **Client Secret** value once. Paste it directly into the managed connection or store it in your organization's secret manager. The `--append` option preserves any existing credentials. [Azure CLI app credentials](https://learn.microsoft.com/en-us/cli/azure/ad/app/credential#az-ad-app-credential-reset)

```bash
az ad app credential reset --id "$CLIENT_ID" --append \
  --display-name "DocuDevs SharePoint" --query password --output tsv
```

The app can access the site after the separate site grant below. Make that grant with an approved Microsoft Graph admin client that has `Sites.FullControl.All`. The DocuDevs app keeps `Sites.Selected`. [Grant an app access to a site](https://learn.microsoft.com/en-us/graph/api/site-post-permissions?view=graph-rest-1.0)

### Find the site, library, and folder IDs

Use an authorized Graph admin client to run the first two requests with your hostname and site path. Copy the complete site `id`, including its comma-separated components. From the drives response, find the entry whose `name` or `webUrl` matches the document library and copy its `id`. For a nested upload folder, get the folder item and copy its `id`. The folder path in that lookup is relative to the library root. [Get a site by path](https://learn.microsoft.com/en-us/graph/api/site-getbypath?view=graph-rest-1.0) · [List a site's drives](https://learn.microsoft.com/en-us/graph/api/drive-list?view=graph-rest-1.0) · [Get a drive item by path](https://learn.microsoft.com/en-us/graph/api/driveitem-get?view=graph-rest-1.0)

```http
GET https://graph.microsoft.com/v1.0/sites/contoso.sharepoint.com:/sites/Finance
GET https://graph.microsoft.com/v1.0/sites/{site-id}/drives
GET https://graph.microsoft.com/v1.0/drives/{drive-id}/root:/Reports/Invoices
```

After you have the site ID, the administrator can grant access with the request below. Use `read` in `roles` if the workflow only reads files.

```http
POST https://graph.microsoft.com/v1.0/sites/{site-id}/permissions
Content-Type: application/json

{
  "roles": ["write"],
  "grantedToIdentities": [
    {"application": {"id": "{application-client-id}", "displayName": "DocuDevs"}}
  ]
}
```

In the **SharePoint** connection, enter **Tenant ID**, **Client ID**, and **Client Secret**. Save **Site ID** and **Library Drive ID** as workflow defaults if you reuse the same library. On **SharePoint Upload**, enter a **Destination Filename** such as `invoices.xlsx` and an optional **Folder Item ID**. Leave the folder blank to use the library root. On **SharePoint List**, the folder field also takes an item ID. **SharePoint Download** takes a path relative to the selected library.

To verify access, run **SharePoint List** against the intended library with the managed connection. For an upload workflow, send a sample file to the test folder and open it in SharePoint.

## Outlook

1. Register a Microsoft Entra app and create its client secret as in the SharePoint registration steps. Use the tenant ID, app client ID, and secret value.
2. Ask your Exchange Online administrator to scope the app to the intended mailbox. Assign **Application Mail.Read** for reading or **Application Mail.Send** for sending, as appropriate. Review any existing tenant-wide grants for the app because those grants can also authorize mailbox access. [Exchange Online RBAC for applications](https://learn.microsoft.com/en-us/exchange/permissions-exo/application-rbac)
3. Create an **Outlook** connection with **Tenant ID**, **Client ID**, and **Client Secret**. Set **Mailbox User** to the mailbox user principal name if the workflows share one mailbox. Otherwise, enter the mailbox on the node. Verify read access with **Outlook List Emails**.

## Google Workspace

1. In Google Cloud Console, enable **Google Drive API** for Drive workflows and **Google Sheets API** for Sheets workflows. Create a service account in the project. [Enable Workspace APIs](https://developers.google.com/workspace/guides/enable-apis)
2. Create and download a JSON key for the service account. In Drive or Sheets, share the file or folder with the service account's email. Use Viewer for downloads, exports, and reads. For uploads, use a shared drive and give the service account permission to add files. Service accounts use shared drives for files they create. [Create a service account and share Workspace files](https://developers.google.com/workspace/guides/create-credentials) · [Shared drive access](https://developers.google.com/workspace/drive/api/guides/about-shareddrives)
3. Create a **Google Workspace** connection and paste the full key into **Service Account JSON**. Configure the file, folder, or spreadsheet ID on the node. Use **Google Drive Export** for native Docs, Sheets, and Slides, and **Google Drive Download** for files stored in Drive. Verify using one accessible sample file. [Drive downloads and exports](https://developers.google.com/workspace/drive/api/guides/manage-downloads)

## Google Cloud Storage

1. Create or select a service account in Google Cloud and download its JSON key. On the target bucket, grant **Storage Object Viewer** for list and download, or **Storage Object Creator** for creating new objects. Choose an object role that includes the required overwrite or delete permissions when your workflow uses those operations. [Cloud Storage IAM roles](https://docs.cloud.google.com/storage/docs/access-control/iam-roles)
2. Create a **Google Cloud Storage** connection. Paste **Service Account JSON** and set **Project ID** for the Google Cloud project used by the connector. Enter the bucket and object path on the workflow node, for example `gs://invoice-intake/inbound/`.
3. Verify by downloading a sample object or uploading a test file under the intended prefix.

## Amazon S3

1. In AWS IAM, create or select an identity approved for this workflow. Grant `s3:ListBucket` on the bucket for listing, `s3:GetObject` on the input objects for download, and `s3:PutObject` on the output objects for upload. Include delete permission only if you configure a move or delete action. [S3 IAM permissions](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_examples_s3_rw-bucket.html)
2. Create an access key for the IAM user and record the key pair. In **Amazon S3** connection setup, enter **Access Key ID** and **Secret Key**. Add the bucket's **Region** and **Bucket** name as reusable defaults. Configure the prefix or object key on the node. [Manage IAM user access keys](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)
3. Verify the intended read or write operation using a dedicated test prefix.

## Azure Blob Storage

1. In Azure Portal, open the storage account. Under **Access keys**, copy the full **Connection string** for an allowed key. This connection form uses a storage connection string. Check that the runtime can reach the Blob endpoint and that account policy permits the selected authorization method. [Azure Storage connection strings](https://learn.microsoft.com/en-us/azure/storage/storage-configure-connection-string)
2. In **Azure Blob Storage** connection setup, paste the **Connection String**. Set **Endpoint** to the account's Blob service URL, such as `https://account.blob.core.windows.net`, and **Container** to the blob container name if used across workflows.
3. Verify by running a list, download, or upload node against a test container or prefix.

## PostgreSQL

1. Have the database administrator create a dedicated login role with a password. Grant database connection and schema usage, then grant `SELECT` on the tables it will query. For **PostgreSQL CopyIn**, grant `INSERT` on the target table and any sequence privileges required by its defaults. [Create a PostgreSQL role](https://www.postgresql.org/docs/current/sql-createrole.html) · [Grant PostgreSQL privileges](https://www.postgresql.org/docs/current/sql-grant.html)
2. Create a **PostgreSQL** connection with **JDBC URL**, **Username**, and **Password**. A TLS URL with server identity verification looks like `jdbc:postgresql://db.example.com:5432/reporting?sslmode=verify-full`. Ensure the runtime has the required CA trust. [pgJDBC TLS settings](https://jdbc.postgresql.org/documentation/ssl/)
3. Run a query against a test table or load a sample CSV into a dedicated CopyIn target.

## MySQL

1. Have the database administrator create an account for the runtime's host or approved network and grant `SELECT` on the required tables. [MySQL account management](https://dev.mysql.com/doc/refman/8.4/en/account-management-statements.html)
2. Create a **MySQL** connection with **JDBC URL**, **Username**, and **Password**. A URL with server identity verification looks like `jdbc:mysql://db.example.com:3306/reporting?sslMode=VERIFY_IDENTITY`. Configure the runtime's CA trust as needed. [MySQL Connector/J TLS settings](https://dev.mysql.com/doc/connector-j/en/connector-j-server-authentication.html)
3. Run a query using the managed connection against a test table.

## SQL Server

1. Have the database administrator create a login using SQL Server authentication and map it to a user in the target database. Grant read access to the required tables or views. [Create a SQL Server login](https://learn.microsoft.com/en-us/sql/relational-databases/security/authentication-access/create-a-login)
2. Create a **SQL Server** connection with **JDBC URL**, **Username**, and **Password**. For example, use `jdbc:sqlserver://db.example.com:1433;databaseName=reporting;encrypt=true;trustServerCertificate=false` with a certificate trusted by the runtime and a matching hostname. [SQL Server JDBC encryption](https://learn.microsoft.com/en-us/sql/connect/jdbc/understanding-ssl-support)
3. Run a query using the managed connection against a test table.

## SFTP

SFTP nodes use runtime settings. Ask the server administrator for the host, port, login, password, and authorized remote directory. Configure `SFTP_HOST`, `SFTP_USERNAME`, and `SFTP_PASSWORD` on the runtime, then set the port and remote path on the node.
