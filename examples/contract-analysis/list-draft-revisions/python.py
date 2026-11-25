import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents

    log = await client.list_draft_revisions(case_id)
    for entry in log.parsed.get("revisions", []):
        print(entry["draftRevision"], entry.get("actorId"), entry.get("reason"))

    earlier = await client.get_draft_revision(case_id, 2)
    print(f"Revision {earlier.parsed['draftRevision']}: {earlier.parsed.get('reason')}")
    # docs:end


asyncio.run(main())
