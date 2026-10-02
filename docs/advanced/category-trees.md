---
title: Category trees and activation
description: Upload versioned category trees, browse them and activate a version so record labels are validated and searchable by subtree.
sidebar_position: 4
---

# Category trees and activation

A category tree gives record labels a hierarchy. Trees are **versioned and immutable**: every upload creates a new draft version. Activating a version makes it the case's active tree. Records reference category codes, not versions, so old results stay readable.

## Upload a tree

`POST /cases/{caseId}/trees` validates and stores a new draft version.

```json
{
  "name": "Catalogue 2026-10",
  "nodes": [
    {"code": "4", "parent": null, "name": "Clothing"},
    {"code": "4.12", "parent": "4", "name": "Socks and tights"},
    {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
    {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"}
  ]
}
```

A node has a `code`, a `parent` code (null for roots), a `name`, an optional `description` that is embedded with the name, a `status` (`active` or `retired`) and, for retired nodes, an optional `successor` code. The response is `201` with `treeVersionId`, `status` (`draft`), `nodes` and `levels`.

Invalid trees are rejected with `400` and nothing is stored. The body lists `problems`, each with a `type` (`CYCLE`, `DUPLICATE_CODE`, `MISSING_PARENT`, `SUCCESSOR_MISSING`, `SUCCESSOR_RETIRED`, `RETIRED_WITH_ACTIVE_CHILD`, `INVALID_STATUS`, `BLANK_CODE`, `TOO_MANY_NODES`), the offending `code` and a `message`.

```python
created = await client.upload_tree(case_id, "tree.json", name="Catalogue 2026-10")
versions = await client.list_trees(case_id)
```

`upload_tree` takes a node list or a path to a JSON file holding the list or `{"name", "nodes"}`. A rejected tree raises `RecordsApiError`; its `body["problems"]` lists the problems.

## Browse a tree

```python
roots = await client.browse_tree_nodes(case_id, version_id)
children = await client.browse_tree_nodes(case_id, version_id, parent="4.12")
hits = await client.browse_tree_nodes(case_id, version_id, q="sock")
node = await client.get_tree_node(case_id, version_id, "4.12.3")
```

`GET /cases/{caseId}/trees/{versionId}/nodes` returns the roots, the children of `parent`, or nodes whose name or code match `q` (case-insensitive) in pages (`page`, `size`). `GET .../nodes/{code}` returns the category with its `path`, `labelPath`, children and siblings.

## Activate a version

`POST /cases/{caseId}/trees/{versionId}/activate` starts a processing job. The active pointer moves only when the job succeeds. While it runs, the previous version stays active.

The job:

1. Validates every record label against the version and reports counts of **valid**, **retired** (with or without a successor) and **missing** codes.
2. Applies your successor mapping (and the successors recorded in the tree), rewriting record labels but keeping `labelSource`.
3. Embeds the category texts so categories can be matched directly. They are never returned as evidence in record search.
4. Re-derives each record's `labelPath`, which powers the `subtree` filter.

Activation is refused, and the report lists the codes, while missing codes remain, unless you set `allowMissing`. Records with missing codes are then excluded as evidence.

```python
job = await client.activate_tree(
    case_id, version_id,
    successor_mapping={"9.90.1": "4.12.3"},
    allow_missing=False,
    wait=True,
)
```

```bash
curl -X POST "https://api.docudevs.ai/cases/42/trees/7/activate" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"successorMapping":{"9.90.1":"4.12.3"},"allowMissing":false}'
# {"jobGuid":"..."}   poll GET /job/status/{guid}
```

Both body fields are optional; send `{}` to activate with the successors already in the tree. `409` means the version is already active or another activation is running. Retired categories are never used as evidence labels.

## CLI

```bash
docudevs trees upload 42 tree.json --name "Catalogue 2026-10"
docudevs trees list 42
docudevs trees node 42 7 4.12.3
docudevs trees activate 42 7 --successor-file successors.json --wait
```
