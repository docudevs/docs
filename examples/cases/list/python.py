import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.list_cases()
    cases = response.parsed

    for case in cases:
        print(f"Case {case.id}: {case.name}")
        if case.description:
            print(f"  Description: {case.description}")
        print(f"  Created: {case.created_at}")
    # docs:end


asyncio.run(main())
