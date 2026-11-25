import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_profile(case_id, view="draft")
    draft = response.parsed

    print(f"Status: {draft['status']}")
    print(f"Draft revision: {draft['draftRevision']}")
    # docs:end


asyncio.run(main())
