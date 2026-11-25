import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    response = await client.get_case(case_id)
    case = response.parsed

    print(f"Case: {case.name}")
    print(f"Description: {case.description}")
    print(f"Created: {case.created_at}")
    print(f"Organization ID: {case.organization_id}")
    # docs:end


asyncio.run(main())
