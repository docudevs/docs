import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_source_authority(case_id)
    authority = response.parsed

    print(f"Revision: {authority['revision']}")
    print(f"Decisions recorded: {len(authority['decisions'])}")
    # docs:end


asyncio.run(main())
