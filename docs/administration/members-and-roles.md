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
