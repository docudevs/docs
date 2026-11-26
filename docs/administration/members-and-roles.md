---
title: Members and Roles
description: Manage organization access in the DocuDevs UI with invites, roles, account states, and the SSO join policy.
sidebar_position: 1
---

# Members and Roles

The **Members** page is the current organization-access surface in the DocuDevs UI. Organization admins use it to invite teammates, assign roles, suspend or restore access, remove members, and define the default role for uninvited users in single-tenant SSO deployments.

## Where to manage access

In the current UI:

1. Sign in to DocuDevs.
2. Open **Settings**.
3. Select **Members**.

Only admins see the **Members** entry in settings.

## Roles

DocuDevs currently exposes three organization roles in the UI.

| Role | Typical use | Main document workspace | Members page | API Keys / Billing pages | Submission area |
| --- | --- | --- | --- | --- | --- |
| `ADMIN` | Organization owners and admins | Yes | Yes | Yes | Yes |
| `MEMBER` | Internal users who work in the main app | Yes | No | No | Yes |
| `SUBMISSION_PORTAL_USER` | Internal users who only need submission flows | No | No | No | Yes |

`SUBMISSION_PORTAL_USER` is still an authenticated user account. It is intended for signed-in submitters who should land in the submission experience instead of the main document workspace.

## Account states

Roles describe *what* someone can do. Member status describes *whether* the account is currently usable.

| Status | What it means |
| --- | --- |
| `INVITED` | The admin has pre-authorized the email address, but the user has not accepted access yet. |
| `ACTIVE` | Normal access. |
| `SUSPENDED` | The user keeps their membership record, but protected app access is blocked. |

## Invite flow

The invite flow in the current build is email-based:

1. An admin enters the member's email address and role on **Settings > Members**.
2. DocuDevs creates the member in `INVITED` state.
3. The user signs in with that same email address.
4. DocuDevs shows an **Accept invitation** screen instead of the normal app shell.
5. After the user accepts, the membership becomes `ACTIVE`.

Until the invitation is accepted, the user cannot access the normal app experience.

DocuDevs also enforces a single membership identity per email address. If that email is already attached to an existing user, the invite is rejected instead of creating a second membership.

### Invite links expire

Each invite link carries a token. Opening it resolves `GET /invites/{token}`, which returns the invited email address, the organization name, and an `expiresAt` timestamp so the acceptance screen can show how long the invite is valid. Once an invite passes `expiresAt` it can no longer be accepted; an admin needs to send a new invite to that address.

## Managing existing members

From the **Team** section on the page, admins can:

- change a member's role
- suspend or reactivate access
- remove a member entirely

DocuDevs protects the last active admin in the organization. In practice, that means the UI and backend both block:

- demoting the final active admin to another role
- suspending the final active admin
- removing the final active admin

This guardrail keeps every organization from locking itself out of admin access.

## Manage members from the API

Everything on the **Members** page is also available over HTTP. These routes need the `ADMIN` role, so an organization API key can call them (an API key acts as an admin and is not itself a member). A signed-in user can call them only as an `ADMIN`; any other role receives `403`. The Python SDK has no helper for these routes, so the examples are cURL only.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/members` | List the organization's members |
| POST | `/members` | Invite a member: `{"email", "role"}` |
| PATCH | `/members/{id}` | Change `role`, `status`, or both |
| DELETE | `/members/{id}` | Remove a member |
| GET, PUT | `/members/join-policy` | Read or set the join policy |
| POST | `/members/accept` | Accept an invitation; needs a signed-in user |

A member has `id`, `email`, `role`, `status`, and, when set, `invitedAt` and `acceptedAt`. Roles are `ADMIN`, `MEMBER` and `SUBMISSION_PORTAL_USER`; statuses are `INVITED`, `ACTIVE` and `SUSPENDED`. An unknown role or status answers `400`.

### List members

<Example name="members/list-members" />

The response is a JSON array of members. The `id` is what the other member routes take in their path.

### Invite a member

<Example name="members/invite-member" />

The route answers `201` with the new member in status `INVITED`. It does not send an email and returns no invitation link, so tell the person yourself. They sign in with the invited email address; until then the membership stays `INVITED`, and an invited account can call only the accept route.

Inviting an email address that already belongs to a user answers `409` with the code `conflict`. The check covers every organization and ignores letter case, so one address can never hold two memberships.

Accepting an invitation (`POST /members/accept`) acts on the signed-in user, so an API key cannot do it: the call answers `400` because there is no user.

### Change a member's role or status

<Example name="members/update-member-role" />

`PATCH` takes `role`, `status`, or both, and answers `200` with the updated member. Set `status` to `SUSPENDED` to block access and back to `ACTIVE` to restore it.

### Remove a member

<Example name="members/remove-member" />

The route answers `204` with no body.

### Last admin protection

An organization must keep at least one member who is both `ADMIN` and `ACTIVE`. When only one such member remains, changing that member's role, suspending them, or removing them answers `409` with the code `last_admin`, and nothing changes. Promote or activate another admin first.

An id that does not exist, or that belongs to another organization, answers `404` with the code `not_found`. Error bodies are `{"code": "...", "message": "..."}`.

### Set the join policy

<Example name="members/set-join-policy" />

The join policy decides what happens when someone signs in through a single-tenant identity provider without an invitation. `ssoDefaultRole` takes one of:

| Value | Effect |
| --- | --- |
| `ADMIN`, `MEMBER`, `SUBMISSION_PORTAL_USER` | Uninvited sign-ins are provisioned with that role |
| `NONE` | Uninvited sign-ins are denied; only invited people can join |
| `null` | Clears the setting. The web app's **Deny (invite only)** choice sends this |

Any other value answers `400`. Both routes answer `200` with `{"ssoDefaultRole": ...}`; the field is absent when no policy is set.

## Role-based app behavior

The current app shell enforces a few role-specific behaviors that are visible in the UI:

- invited users see an invitation-acceptance gate
- suspended users see a suspension message instead of app content
- `SUBMISSION_PORTAL_USER` accounts are redirected into the submission area
- non-admins are redirected away from **Settings > Members**

## SSO join policy on the Members page

The bottom card on the page is labeled **SSO join policy**.

This setting is only relevant for deployments that already authenticate users through a single-tenant identity provider outside the normal DocuDevs invite flow. It controls the default role granted to a user who signs in through that identity provider without being explicitly invited first.

Available choices in the current UI are:

- `ADMIN`
- `MEMBER`
- `SUBMISSION_PORTAL_USER`
- `Deny (invite only)`

If you choose **Deny**, uninvited identity-provider users are blocked until an admin explicitly creates a membership for them.

For more detail on the current SSO behavior and scope, see [Single Sign-On](./single-sign-on).
