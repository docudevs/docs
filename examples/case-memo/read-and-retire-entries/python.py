import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    entry_id = "m_88"  # id returned when the entry was created

    memo = await client.list_memo(case_id, status="active")
    for entry in memo.get("items") or []:
        print(entry["id"], entry["type"], entry["status"])

    history = await client.list_memo_revisions(case_id, entry_id)
    for revision in history.get("items") or []:
        print(f"revision {revision['revision']}: {revision['status']}")

    # Retiring adds a revision; earlier versions stay readable
    retired = await client.retire_memo_entry(case_id, entry_id)
    print(retired["id"], retired["status"], retired["memoVersion"])
    # docs:end


asyncio.run(main())
