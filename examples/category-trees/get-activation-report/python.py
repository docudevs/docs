import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
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
    # docs:end


asyncio.run(main())
