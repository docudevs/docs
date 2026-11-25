import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of the case that holds the playbook documents

    with open("playbook.json") as source:  # a playbook in the playbook format
        playbook = json.load(source)

    draft = await client.get_contract_analysis_profile(case_id, view="draft")
    revision = draft.parsed["draftRevision"]  # a new playbook is at revision 0

    imported = await client.import_playbook(case_id, playbook, draft_revision=revision)
    if imported.status_code == 422:
        # A playbook with mistakes is refused with every problem and its JSON path
        for issue in imported.parsed["validation"].get("errors", []):
            print(issue["path"], issue["code"], issue["message"])
    elif imported.status_code == 200:
        print(f"Draft revision: {imported.parsed['draftRevision']}")
    else:
        raise RuntimeError(f"Import failed with status {imported.status_code}")


asyncio.run(main())
