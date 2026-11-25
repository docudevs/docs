import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.get_case(case_id=99999)
    if response.status_code == 404:
        print("Case not found")
    # docs:end


asyncio.run(main())
