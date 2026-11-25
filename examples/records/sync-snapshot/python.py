import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with a configured dataset

    # Raises RecordsImportFailed when an import fails, and TimeoutError after 1800 seconds
    dry = await client.sync_records(case_id, "snapshot.ndjson", wait=True)
    summary = dry.get("syncSummary")
    if summary is None:
        raise RuntimeError("The dry run produced no report")
    print(summary["counts"])
    print(summary.get("guard"))

    # After reviewing the report, apply the same file for real
    applied = await client.confirm_sync_records(
        case_id, "snapshot.ndjson", report_id=dry["importId"], wait=True
    )
    print(applied["status"], applied["recordsAdded"], applied["recordsDeleted"])
    # docs:end


asyncio.run(main())
