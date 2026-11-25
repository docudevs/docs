import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_profile(case_id, view="published")
    active = response.parsed

    print(f"Status: {active['status']}")
    print(f"Published revision: {active['publishedRevision']}")
    # docs:end


asyncio.run(main())
