---
title: Azure Marketplace Private Networking
description: Prerequisites, installation, verification, and troubleshooting for installing DocuDevs from Azure Marketplace into your own virtual network with private-only ingress.
sidebar_position: 3
---

# Azure Marketplace Private Networking

This guide explains how to install DocuDevs from Azure Marketplace into an
existing virtual network (BYO VNet, private-only mode), and how to operate,
verify, upgrade, and troubleshoot that installation.

## What private-only means

When you select **Existing VNet - private only** in the Marketplace wizard:

- The DocuDevs UI and API are reachable only from your selected virtual
  network and the networks connected to it (peering, VPN, ExpressRoute).
  There is no public ingress.
- PostgreSQL, Storage, Azure AI Search, Azure OpenAI, and Document
  Intelligence are reachable only through private endpoints in your network.
- The Container Apps environment is internal. DocuDevs never declares or
  modifies your network resources; your VNet, subnets, NSGs, route tables,
  firewalls, and DNS remain entirely yours.
- Private ingress does not remove authentication. All DocuDevs API and UI
  calls still require Microsoft Entra authentication, and internal API callers
  must still authenticate like any other client. Network location is not
  authorization.

The deployment fails closed: if a private-networking requirement cannot be
satisfied, the deployment stops with a diagnostic. It never responds to a
private-networking failure by enabling public access.

The wizard shows this reminder when you select private-only mode:

> Private ingress stays inside your virtual network, but the deployment still requires controlled outbound HTTPS for publisher image pulls from the DocuDevs publisher ACR and layer endpoints, Container Apps platform dependencies, Microsoft Entra identity, and public Azure Monitor ingestion egress. A network policy that blocks these destinations will fail the deployment. Review the [private network guide](https://docs.docudevs.ai/docs/administration/azure-marketplace-private-network) before continuing.

## Supported topology and immutable choices

Private-only mode supports one topology:

- One customer-owned VNet in the **same subscription** as the Managed
  Application and in the **same Azure region** as the deployment. The VNet may
  live in a different resource group.
- Two pre-existing subnets you own:
  1. A dedicated Container Apps infrastructure subnet delegated to
     `Microsoft.App/environments`, containing no unrelated resources.
  2. A private-endpoint subnet for the DocuDevs-owned private endpoints. You
     keep ownership of any NSG, route table, and private-endpoint network
     policy attached to it.
- Either DocuDevs-managed or customer-managed PaaS private DNS zones.
  Customer-managed zones may be in another subscription within the same
  Microsoft Entra tenant.

These choices are **immutable** after installation:

- The network mode (`managed` vs `existingPrivate`), the VNet ID, and both
  subnet IDs cannot be changed. A redeployment that tries to change them fails
  explicitly instead of replacing the Container Apps environment.
- Moving between managed networking and BYO VNet — or to another VNet —
  requires a new installation and data migration.
- The first release uses the generated Azure Container Apps domain and
  certificate. Customer-defined internal domains and certificates are not yet
  supported.

## Before you install

Complete all of the following before starting the Marketplace wizard:

1. A VNet and two subnets prepared as described above (see
   [Prepare the subnets](#prepare-the-container-apps-and-private-endpoint-subnets)).
2. A user-assigned managed identity authorized as described in
   [Create and authorize the deployment identity](#create-and-authorize-the-deployment-identity).
3. Resource providers `Microsoft.ContainerInstance` and `Microsoft.Storage`
   registered in the deployment region, because control-plane preflight runs
   as an Azure deployment script on public-network Azure Container Instances
   with a supporting storage account.
4. Subscription Azure Policy that allows that public-network deployment-script
   execution. Supplying a private-only storage account to the deployment
   script is not a fallback: Azure deployment scripts require public
   networking and do not support storage-account firewall rules for their
   execution storage.
5. Controlled public HTTPS egress (TCP 443) from the Container Apps subnet to
   the public endpoints listed in
   [Configure controlled outbound connectivity](#configure-controlled-outbound-connectivity),
   plus private-endpoint reachability for the provisioned PaaS services. The
   PostgreSQL readiness probe uses TCP-only port 5432; the other private PaaS
   probes use HTTPS on port 443.
6. A workstation, jump host, or VPN path with private DNS resolution and
   network reachability into the VNet, so you can complete post-installation
   setup and verification.
7. A single-tenant Microsoft Entra application for sign-in, as required by the
   standard Marketplace installation flow.

## Create and authorize the deployment identity

Existing-resource deployment requires a **user-assigned managed identity**
that you create before installation. In the wizard's Networking step, the
**Deployment identity** selector accepts exactly one identity: if several are
selected, deployment cannot proceed predictably. The identity runs
control-plane preflight and deployment scripts against your linked resources;
it is never attached to DocuDevs runtime applications.

Two principal boundaries apply to the deployment script. The
installing/deployment principal and the Managed Application authorization it
uses must be allowed to create
`Microsoft.Resources/deploymentScripts` and the service-created temporary
public-network Container Instance and storage resources in the managed
resource group. The selected customer user-assigned identity is attached to
that script for the linked-resource ARM operations and still needs the subnet,
customer-DNS, and read permissions listed below. Register the required
providers and ensure Azure Policy permits those temporary public resources.
Do not attach this identity to the API, UI, worker, or jobs, and do not try to
provide private-only execution storage for the deployment script.

Grant the identity these role assignments:

| Role | Scope |
| --- | --- |
| **Network Contributor** | The selected Container Apps subnet and the selected private-endpoint subnet (preferably scoped to those two subnets). |
| **Private DNS Zone Contributor** | Every customer-managed private DNS zone you select in the wizard. |
| **Reader** | The containing VNet and any referenced resource groups where the narrower assignments do not already provide read access. |

Notes:

- Permissions inherited through groups, deny assignments, and custom roles may
  not be verifiable before deployment. The preflight distinguishes incomplete
  permission verification from a confirmed failure; a later Azure
  authorization failure identifies the identity, resource, and required
  operation.
- **Retain the identity after installation.** Upgrades reuse it. Deleting it
  breaks future upgrades.

Example creation and assignment:

```bash
RG=my-docudevs-vnet-rg
LOCATION=switzerlandnorth

az identity create --name docudevs-deploy-identity --resource-group "$RG" --location "$LOCATION"

PRINCIPAL_ID=$(az identity show \
  --name docudevs-deploy-identity --resource-group "$RG" \
  --query principalId -o tsv)

az role assignment create \
  --assignee-object-id "$PRINCIPAL_ID" \
  --assignee-principal-type ServicePrincipal \
  --role "Network Contributor" \
  --scope "$CA_SUBNET_ID"
az role assignment create \
  --assignee-object-id "$PRINCIPAL_ID" \
  --assignee-principal-type ServicePrincipal \
  --role "Network Contributor" \
  --scope "$PE_SUBNET_ID"
az role assignment create \
  --assignee-object-id "$PRINCIPAL_ID" \
  --assignee-principal-type ServicePrincipal \
  --role "Private DNS Zone Contributor" \
  --scope "$CUSTOMER_DNS_ZONE_ID"
```

## Prepare the Container Apps and private-endpoint subnets

Container Apps environment subnet:

- Dedicated to the Container Apps environment: no other resources.
- Delegated to `Microsoft.App/environments`.
- **At least `/27` (minimum); `/24` or larger is recommended** for operational
  headroom.
- In the same region as the deployment.

Private-endpoint subnet:

- Suitable for the DocuDevs-owned private endpoints (PostgreSQL, Blob,
  Queue, File, Azure AI Search, Azure OpenAI, Document Intelligence).
- Sized for those endpoints plus platform overhead.
- You configure and own its NSG, route table, and private-endpoint network
  policies. DocuDevs does not modify them.

DocuDevs references both subnets as existing resources. The deployment never
declares them as owned resources, changes their address spaces, alters their
delegation or policy configuration, or deletes them at uninstall time.

## Choose DocuDevs-managed or customer-managed private DNS

The wizard's **PaaS private DNS ownership** option controls who owns the
canonical PaaS private DNS zones:

- **DocuDevs-managed private DNS** (default): DocuDevs creates and links the
  PaaS private DNS zones in the deployment resource group. Choose this if you
  do not already operate matching zones.
- **Customer-managed private DNS**: you select your own existing private DNS
  zones for each service. The zones must use the expected canonical names, and
  your deployment identity needs **Private DNS Zone Contributor** on every
  zone you select. Customer-managed zones may be in another subscription in
  the same Entra tenant.

The wizard asks for these customer-managed zones:

- Storage blob private DNS zone (`privatelink.blob.core.windows.net`)
- Storage queue private DNS zone (`privatelink.queue.core.windows.net`)
- Storage file private DNS zone (`privatelink.file.core.windows.net`)
- PostgreSQL private DNS zone (`privatelink.postgres.database.azure.com`)
- Azure OpenAI private DNS zone (`privatelink.openai.azure.com`)
- Azure AI Search private DNS zone (`privatelink.search.windows.net`)
- Document Intelligence private DNS zone (`privatelink.cognitiveservices.azure.com`)

The generated Container Apps default-domain zone always remains
DocuDevs-managed regardless of this choice.

If your VNet uses custom DNS servers (not Azure-provided resolution), make
sure they forward `*.core.windows.net`, `*.database.azure.com`,
`*.openai.azure.com`, `*.search.windows.net`,
`*.cognitiveservices.azure.com`, and the generated Container Apps domain to
Azure DNS (for example through an in-VNet DNS forwarder or DNS private
resolver), or the private endpoints will not resolve.

## Configure controlled outbound connectivity

Private-only deployments need two distinct network paths. Public publisher,
platform, Entra, and Azure Monitor destinations require controlled outbound
HTTPS on TCP 443 from the Container Apps subnet. The provisioned PaaS private
endpoints require private DNS resolution and reachability on their service
ports: PostgreSQL is probed with TCP-only port 5432, while Storage, Search,
OpenAI, and Document Intelligence are probed over HTTPS on port 443. A
firewall, NSG, forced-tunnel design, private DNS configuration, or private
endpoint route that blocks any required destination fails the deployment at a
named gate.

### 1. Publisher registry (release-specific)

Lead the allowlist with the publisher registry endpoints for this release:

:::caution Restricted private-preview qualification

The current restricted BYO VNet qualification package uses the existing
Standard publisher registry in Sweden Central. Standard ACR does not expose a
dedicated regional data endpoint, so qualification networks must allow all of
the following outbound paths on TCP 443:

- `ddmkt7d52dee38b83.azurecr.io`
- `*.blob.core.windows.net`
- the `Storage.swedencentral` service tag, where the firewall supports Azure
  service-tag rules

This broader Blob Storage allowance is a temporary qualification contract,
not the intended production allowlist. Use it only for the restricted preview
package whose endpoint manifest has an empty `dataEndpoints` array. A later
production release will move the publisher registry to Premium and publish
dedicated data endpoints so customers can replace the broad Blob allowance
with exact release-specific endpoints.

:::

The release endpoint source of truth is
`infra/marketplace/release-network-endpoints.json` in the DocuDevs application
repository. The currently checked-in manifest has `releaseVersion`
`v0.0.0-unreleased`, and
`infra/marketplace/release-network-endpoints.PLACEHOLDER.md` is still present.
That combination is fixture-only, non-publishable metadata, not a production
customer allowlist. Do not use the current table to authorize a production
installation.

| Destination | Purpose |
| --- | --- |
| `ddmkt7d52dee38b83.azurecr.io` | Publisher ACR login server (image pulls). |
| `ddmkt7d52dee38b83.switzerlandnorth.data.azurecr.io` | Publisher ACR layer-data endpoint. |

Anonymous cross-tenant image pulls from these registries are supported by the
platform, but they still traverse the data plane: both the login server and
the region-specific layer-data endpoint must be allowed, or pulls stall in
`ErrImagePull` / `ImagePullBackOff`. Always allow the release-specific
endpoints published for the version you install — they can change between
releases. Because the publisher registry belongs to a different tenant, a
customer-side private endpoint to it is not available; controlled outbound
HTTPS from the delegated subnet is the only supported path.

For an approved release, the release workflow must first upgrade the publisher
ACR to Premium with dedicated data endpoints, query those endpoints with
`az acr show-endpoints`, and regenerate the manifest. The release-specific
`releaseVersion` and endpoint values must then be copied into this guide,
validated with `npm run typecheck` and `npm run build`, and committed before
the documentation or Marketplace package is published. The repository
placeholder marker is removed only as part of that approved release refresh.

### 2. Platform, Microsoft Container Registry, and Entra FQDNs

| Destination | Purpose |
| --- | --- |
| `mcr.microsoft.com` | Microsoft Container Registry. |
| `*.data.mcr.microsoft.com` | MCR layer-data delivery. |
| `acs-mirror.azureedge.net` | Container Apps platform components. |
| `packages.aks.azure.com` | Container Apps platform packages. |
| `login.microsoftonline.com` | Microsoft Entra authentication. |
| `*.identity.azure.net` | Microsoft Entra Workload Identity federation. |

### 3. Service tags

Where your firewall supports FQDN/service-tag rules, also allow:

- `MicrosoftContainerRegistry`
- `AzureActiveDirectory`
- `AzureFrontDoor.FirstParty`
- `AzureMonitor`

### Operational implications

- **Forced tunneling:** if a UDR sends 0.0.0.0/0 to a firewall, NVA, or proxy,
  every destination above must be explicitly permitted there. The
  network-readiness gate reports which destination category failed, but the
  cleanest result is to allow the list up front.
- **NSG rules** on the Container Apps subnet must permit outbound TCP 443 to
  the public destinations above and private PaaS HTTPS endpoints, plus TCP
  5432 to the PostgreSQL private endpoint; service-tag-based NSG rules cover
  the tagged public categories, but the publisher ACR and MCR FQDNs need
  firewall/proxy-level FQDN rules because NSGs cannot filter by FQDN.
- **Public Azure Monitor ingestion and query** endpoints are reached over the
  public internet even in private-only deployments (see the next section).

## Understand monitoring traffic and the first-release AMPLS limitation

In the first release:

- Telemetry is sent to a DocuDevs-provisioned Log Analytics workspace and
  Application Insights component over **public Azure Monitor ingestion**
  endpoints, and portal queries also use public Azure Monitor endpoints. This
  is outbound egress from your VNet; no monitoring data is exposed through
  public inbound routes to DocuDevs.
- The workspace and component are **not automatically attached to your Azure
  Monitor Private Link Scope (AMPLS)** in the first release. If your
  organization enforces deny-all-public-monitoring policy, coordinate an
  exception for this egress before installing, or the Azure Monitor probe and
  telemetry will fail.
- The in-environment connectivity probe treats blocked Azure Monitor egress as
  a failure and says so explicitly: first-release telemetry requires approved
  public Azure Monitor egress.

Plan to revisit AMPLS integration in a later release.

## Install from Azure Marketplace

1. Open the DocuDevs Managed Application offer and start the installation.
2. Complete the Basics, organization, authentication, and model steps.
3. In the **Networking** step:
   - Set **Network mode** to **Existing VNet - private only**.
   - Select your virtual network.
   - Select the Container Apps environment subnet (**at least /27**) and the
     private-endpoint subnet.
   - Select exactly one **Deployment identity**.
   - Choose **PaaS private DNS ownership**, and if customer-managed, select
     all seven zones.
4. Review and create. The deployment then runs these ordered phases:
   1. Public-network deployment-script preflight validating the identity,
      VNet, subnets, and selected DNS zones through Azure Resource Manager.
   2. Network ID resolution and private DNS zone creation or reuse.
   3. Internal Container Apps environment creation and its generated private
      ingress DNS zone.
   4. Provisioning of private PaaS resources and their private endpoints.
   5. A minimal digest-pinned network-readiness job started from the publisher
      ACR. The image pull itself tests publisher-registry and layer-data
      egress; after starting, the job probes the documented MCR/platform,
      Entra, and Azure Monitor destinations plus the private PostgreSQL,
      Storage, Search, OpenAI, and Document Intelligence endpoints, and fails
      closed before bootstrap when a required dependency is unreachable.
   6. PostgreSQL bootstrap, password removal, application, search-index, and
      optional model-canary phases.
   7. Final ARM resource-contract verification and non-secret outputs.

Do not delete the deployment identity, subnets, or DNS zones while the
deployment is running or afterwards (see
[Upgrade and uninstall safely](#upgrade-and-uninstall-safely)).

## Verify private DNS, UI, API, setup URI, and MSAL callback

Run these checks from a machine connected to the VNet or a reachable private
network.

### Private DNS resolution

```bash
az network private-endpoint show \
  --name <private-endpoint-name> \
  --resource-group <managed-resource-group> \
  --query "{name:name, subnetId:properties.subnet.id, customDnsConfigs:properties.customDnsConfigs}"

nslookup <postgres-server-name>.privatelink.postgres.database.azure.com
nslookup <generated-container-apps-domain>
```

Each name should resolve to a private IP address inside your address space,
not a public address. If names resolve publicly, check VNet links on the
private DNS zones and your custom DNS forwarding.

### UI, API, setup URI, and MSAL callback

The deployment emits two non-secret outputs:

- `postSetupUri`: `<ui-uri>/setup`
- `msalCallbackUri`: `<ui-uri>/api/auth/msal-callback`

1. Resolve the generated Container Apps domain (above).
2. Open the private `postSetupUri` in your browser and complete initial
   setup.
3. Register the exact private `msalCallbackUri` as a redirect URI in your
   Entra application. The callback uses the private Container Apps hostname;
   Microsoft Entra does not need a public inbound route to it, but your
   browser must have private DNS and reachability.
4. Complete an authenticated UI login.
5. Complete an authenticated API request from an internal client, without
   printing secrets:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer $DOCUDEVS_TOKEN" \
  "https://<generated-container-apps-domain>/api/documents"
```

An HTTP status in the 2xx/4xx range proves reachability and authentication
behavior; connection timeouts or DNS failures indicate a networking problem.

## Upgrade and uninstall safely

Upgrades:

- Keep the deployment identity and all of its role assignments. Upgrades run
  under the same identity contract as installation.
- Redeployments preserve linked customer resources and are idempotent.
- Do not change the network mode, VNet ID, or subnet IDs between upgrades;
  such a change fails explicitly rather than replacing the environment.

Uninstall (delete the Managed Application):

- Removed: DocuDevs-owned resources, private endpoints, endpoint-owned DNS
  records, zone groups, and the generated Container Apps DNS zone.
- Not removed: your VNets, subnets, private DNS zones, VNet links, route
  tables, NSGs, firewalls, and the deployment identity. Clean those up
  yourself when you no longer need them.

## Troubleshoot by deployment phase

Failures are reported per phase. Use these diagnostics without exposing
secrets:

```bash
az resource show \
  --ids <failed-resource-id> \
  --api-version 2022-09-01

az containerapp env show \
  --name <environment-name> \
  --resource-group <managed-resource-group>

az containerapp job execution list \
  --job <network-readiness-job-name> \
  --resource-group <managed-resource-group>

az network private-endpoint show \
  --name <private-endpoint-name> \
  --resource-group <managed-resource-group>

nslookup <host-under-test>
curl -sS -o /dev/null -w 'dns+tls+tcp: %{http_code} time: %{time_total}s\n' \
  "https://<host-under-test>"
```

| Phase | What it tells you |
| --- | --- |
| Selection | Incompatible scope, location, or subnet capacity was chosen. Fix the wizard selection. |
| Control-plane preflight | Problems with delegation, subnet occupancy, DNS zone selection, identity access, deployment-script policy, ACI availability, or the temporary storage account. Register `Microsoft.ContainerInstance` and `Microsoft.Storage` and confirm policy allows public-network deployment scripts. |
| Environment provisioning | An Azure operation was rejected; missing Container Apps platform, DNS, NSG, or UDR requirements are called out when the environment cannot become ready. |
| Publisher image-pull gate | A recognized `ErrImagePull`, `ImagePullBackOff`, or registry-timeout condition produces exactly this message: **`DocuDevs image pull failed: outbound HTTPS to ddmkt7d52dee38b83.azurecr.io or its ACR layer-data endpoint is blocked. Allow the release-specific publisher ACR endpoints from the Container Apps subnet, then retry the deployment.`** The diagnostic includes the image digest and the latest non-secret ARM condition. If the pull did not reach a registry-timeout diagnosis before the gate budget elapsed, the gate instead reports that either the publisher registry (`ddmkt7d52dee38b83.azurecr.io`) or Container Apps platform egress may be blocked. |
| In-environment connectivity | Reports the destination category and hostname, DNS result, TCP/TLS outcome, and the likely NSG, UDR, firewall, proxy, or resolver cause, without exposing credentials. If the job itself failed without a registry signal, the gate reports: `DocuDevs network readiness job <JOB_NAME> failed. Open the Container Apps job logs for <JOB_NAME> in the Container Apps environment to find which probe target failed; this gate reads only ARM execution status and cannot read job stdout.` An Azure Monitor probe failure means first-release telemetry needs approved public Azure Monitor egress. |
| Post-deployment client access | Failures limited to your workstation, peered network, VPN, ExpressRoute, or DNS-forwarding path — not the deployment. Re-run the [verification steps](#verify-private-dns-ui-api-setup-uri-and-msal-callback) from a correctly connected client. |

Remember: a private-networking failure never causes the deployment to enable
public access. Fix the named destination or permission and retry.
