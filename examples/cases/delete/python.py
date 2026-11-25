import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    response = await client.delete_case(case_id)

    if response.status_code == 204:
        print("Case deleted successfully")
    # docs:end


asyncio.run(main())
