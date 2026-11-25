import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case

    result = await client.list_case_summaries(case_id=case_id)
    for doc in result.parsed:
        print(f"{doc['filename']}: {doc['summary']['document_type']}")
    # docs:end


asyncio.run(main())
