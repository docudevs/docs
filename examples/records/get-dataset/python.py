import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    dataset = await client.get_dataset(case_id)
    print(f"Mapping version: {dataset['mappingVersion']}")
    print(f"Active tree version: {dataset.get('activeTreeVersionId')}")
    # docs:end


asyncio.run(main())
