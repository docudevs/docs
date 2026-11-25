import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.list_templates()
    for template in response.parsed:
        print(f"Name: {template.name}, Created: {template.created_at}")
    # docs:end


asyncio.run(main())
