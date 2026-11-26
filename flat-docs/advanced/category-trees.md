---
title: Category trees and activation
description: Upload versioned category trees, browse them and activate a version so record labels are validated and searchable by subtree.
sidebar_position: 4
---
<!-- Generated from docs/advanced/category-trees.md. Do not edit by hand. -->

# Category trees and activation

A category tree gives record labels a hierarchy. Trees are **versioned and immutable**: every upload creates a new draft version. Activating a version makes it the case's active tree. Records reference category codes, not versions, so old results stay readable. A version is `draft`, `active` or `superseded` (it was active and a later version replaced it). With an organization API key, the case must be one that the key can use; see [Which cases you can use](./case-agent.md#which-cases-you-can-use).

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

**cURL**

```bash
CASE_ID=42  # id of a records case

STATUS=$(curl -s -S -o tree-response.json -w '%{http_code}' -X POST "https://api.docudevs.ai/cases/$CASE_ID/trees" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Catalogue 2026-10",
    "nodes": [
      {"code": "4", "parent": null, "name": "Clothing"},
      {"code": "4.12", "parent": "4", "name": "Socks and tights"},
      {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
      {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"}
    ]
  }')

if [ "$STATUS" != "201" ]; then
  # A rejected tree stores nothing; the body lists every problem
  jq -r '.problems[]? | "\(.type) \(.code // "-") \(.message)"' tree-response.json >&2
  exit 1
fi
jq '{treeVersionId, status, nodes, levels}' tree-response.json
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient, RecordsApiError


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    nodes = [
        {"code": "4", "parent": None, "name": "Clothing"},
        {"code": "4.12", "parent": "4", "name": "Socks and tights"},
        {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
        {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"},
    ]

    try:
        created = await client.upload_tree(case_id, nodes, name="Catalogue 2026-10")
    except RecordsApiError as error:
        # A rejected tree stores nothing; its body lists every problem
        for problem in error.body.get("problems", []):
            print(problem["type"], problem.get("code"), problem["message"])
        raise
    print(f"Tree version {created['treeVersionId']}: {created['status']}, {created['nodes']} nodes")


asyncio.run(main())
```

**CLI**

```bash
CASE_ID=42  # id of a records case

cat > tree.json <<'JSON'
{
  "name": "Catalogue 2026-10",
  "nodes": [
    {"code": "4", "parent": null, "name": "Clothing"},
    {"code": "4.12", "parent": "4", "name": "Socks and tights"},
    {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
    {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"}
  ]
}
JSON

docudevs trees upload "$CASE_ID" tree.json
```

The Python client's `upload_tree` takes a node list or a path to a JSON file holding the list or `{"name", "nodes"}`. A rejected tree raises `RecordsApiError`; its `body["problems"]` lists the problems. The CLI reads the same file.

List the versions of a case, newest first, with `GET /cases/{caseId}/trees`. Each entry has `treeVersionId`, `status`, `nodes`, `levels` and a `contentHash`.

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees" \
  -H "Authorization: $API_KEY" | jq -r '(.items // [])[] | "\(.treeVersionId) \(.status) \(.nodes) \(.levels)"'
```

**Python**

```python
case_id = 42  # id of a records case

versions = await client.list_trees(case_id)
for version in versions.get("items", []):
    print(version["treeVersionId"], version["status"], version["nodes"], version["levels"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs trees list "$CASE_ID"
```

## Browse a tree

**cURL**

```bash
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

ROOTS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY")
CHILDREN=$(curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "parent=4.12")
HITS=$(curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "q=sock")

echo "$HITS" | jq -r '(.items // [])[] | "\(.code) \(.name) \(.status) \(.childCount)"'
echo "$HITS" | jq -r '"\(.total) matches"'
echo "$ROOTS" | jq -r '"\(.items // [] | length) roots"'
echo "$CHILDREN" | jq -r '"\(.items // [] | length) children of 4.12"'
```

**Python**

```python
case_id = 42  # id of a records case
version_id = 7  # treeVersionId returned when the tree was uploaded

roots = await client.browse_tree_nodes(case_id, version_id)
children = await client.browse_tree_nodes(case_id, version_id, parent="4.12")
hits = await client.browse_tree_nodes(case_id, version_id, q="sock")

for node in hits.get("items", []):
    print(node["code"], node["name"], node["status"], node["childCount"])
print(f"{hits['total']} matches")
print(f"{len(roots.get('items', []))} roots")
print(f"{len(children.get('items', []))} children of 4.12")
```

`GET /cases/{caseId}/trees/{versionId}/nodes` returns the roots, the children of `parent`, or nodes whose name or code match `q` (case-insensitive) in pages (`page`, `size`). The CLI has no browse command; read one category with `docudevs trees node`. `GET .../nodes/{code}` returns the category with its `path`, `labelPath`, children and siblings.

**cURL**

```bash
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes/4.12.3" \
  -H "Authorization: $API_KEY" | jq '{code, name, path, children: [(.children // [])[].code], siblings: [(.siblings // [])[].code]}'
```

**Python**

```python
case_id = 42  # id of a records case
version_id = 7  # treeVersionId returned when the tree was uploaded

node = await client.get_tree_node(case_id, version_id, "4.12.3")
print(" > ".join(node["path"]))
print([child["code"] for child in node.get("children", [])])
print([sibling["code"] for sibling in node.get("siblings", [])])
```

**CLI**

```bash
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

docudevs trees node "$CASE_ID" "$VERSION_ID" 4.12.3
```

## Activate a version

`POST /cases/{caseId}/trees/{versionId}/activate` starts a processing job. The active pointer moves only when the job succeeds. While it runs, the previous version stays active.

The job:

1. Validates every record label against the version and reports counts of **valid**, **retired** (with or without a successor) and **missing** codes.
2. Applies your successor mapping (and the successors recorded in the tree), rewriting record labels but keeping `labelSource`.
3. Embeds the category texts so categories can be matched directly. They are never returned as evidence in record search.
4. Re-derives each record's `labelPath`, which powers the `subtree` filter.

Activation is refused, and the report lists the codes, while missing codes remain, unless you set `allowMissing`. Records with missing codes are then excluded as evidence.

**cURL**

```bash
wait_for_activation() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) echo "Activation job $1: $STATUS"; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Activation job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Activation job $1 did not complete in time" >&2
  return 1
}

CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/activate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"successorMapping": {"9.90.1": "4.12.3"}, "allowMissing": false}' | jq -r '.jobGuid')

wait_for_activation "$JOB_GUID"
```

**Python**

```python
case_id = 42  # id of a records case
version_id = 7  # treeVersionId returned when the tree was uploaded

# Raises RecordsImportFailed when the job fails, and TimeoutError after 3600 seconds
job = await client.activate_tree(
    case_id,
    version_id,
    successor_mapping={"9.90.1": "4.12.3"},
    allow_missing=False,
    wait=True,
)
print(f"Activation job {job['jobGuid']}: {job['status']}")
```

**CLI**

```bash
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

echo '{"9.90.1": "4.12.3"}' > successors.json

docudevs trees activate "$CASE_ID" "$VERSION_ID" --successor-file successors.json --wait
```

Both body fields are optional; send `{}` to activate with the successors already in the tree. The response carries the `jobGuid` to poll with `GET /job/status/{guid}` and the `importId` of the activation. `409` means the version is already active or another activation is running. A refused activation ends the job with an error; read its report with `GET /cases/{caseId}/trees/{versionId}/activations/{importId}/report` (`docudevs trees activation-report`). Retired categories are never used as evidence labels.

The report holds the `status`, the `counts` of valid, retired and missing record labels, the `successorRewrites` that were applied, the `missing` codes and the `retiredWithoutSuccessor` codes (each with a sample of record keys), and the `memoScopeIssues`: memo entries whose scope names categories that are not valid in the version. The route answers `404` until the report is ready.

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded
JOB_GUID="<job-guid>"  # jobGuid returned when the version was activated

# Returns when the job is COMPLETED; exits when it ends ERROR, FAILED or TIMEOUT, or when 600 seconds pass first
wait_for_job "$JOB_GUID"

# Find the activation among the case's imports (newest first). This reads one page
# of up to 200; an older activation may need the continuation token of the page.
IMPORT_ID=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports?limit=200" \
  -H "Authorization: $API_KEY" \
  | jq -r --arg guid "$JOB_GUID" 'first((.items // [])[] | select(.jobGuid == $guid) | .importId) // empty')

if [ -z "$IMPORT_ID" ]; then
  echo "No import found for job $JOB_GUID" >&2
  exit 1
fi

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/activations/$IMPORT_ID/report" \
  -H "Authorization: $API_KEY" \
  | jq '{status, counts, memoScopeIssues: (.memoScopeIssues // [] | length)}'
```

**Python**

```python
case_id = 42  # id of a records case
version_id = 7  # treeVersionId returned when the tree was uploaded
job_guid = "<job-guid>"  # jobGuid returned when the version was activated

# Returns when the job is COMPLETED; raises RecordsImportFailed when it ends ERROR or TIMEOUT,
# and TimeoutError after 600 seconds (a job that ends FAILED is not detected, so this waits out the 600 seconds)
await client.wait_for_records_job(job_guid, timeout=600, poll_interval=5)

# Find the activation among the case's imports (newest first). This reads one page
# of up to 200; an older activation may need the continuation token of the page.
page = await client.list_imports(case_id, limit=200)
import_id = next(
    (item["importId"] for item in page.get("items", []) if item.get("jobGuid") == job_guid),
    None,
)
if import_id is None:
    raise SystemExit(f"No import found for job {job_guid}")

report = await client.get_tree_activation_report(case_id, version_id, import_id)
print(report.get("status"), report.get("counts"))
print(f"Memo entries to review: {len(report.get('memoScopeIssues', []))}")
```

The example waits for the activation, finds its import in the list of the case's imports by `jobGuid`, and reads the report. When you kept the `importId` from the activation response, skip the lookup. The example stops when the job fails; to read the report of a refused activation, skip the wait. The command-line client has no wait step: `docudevs trees activation-report` takes the case, version and import ids.
