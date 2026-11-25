import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents

    # Raises an exception when the export is refused
    playbook = await client.export_playbook(case_id, view="draft")

    with open("playbook.json", "w") as target:
        json.dump(playbook, target, indent=2)
    print(f"Clauses: {len(playbook.get('clauses', []))}")
    # docs:end


asyncio.run(main())
