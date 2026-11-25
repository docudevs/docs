import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    entry = await client.add_memo_entry(
        case_id,
        "instruction",
        {"kind": "subtree", "code": "7.31"},
        "Fiction books are filed by genre when the description names one.",
    )
    print("Proposed entry", entry["id"])

    # Nothing changes in classification until the instruction is approved
    approved = await client.approve_memo_entry(case_id, entry["id"])
    print(approved["id"], approved["status"], approved["memoVersion"])


asyncio.run(main())
