import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case

    result = await client.list_case_summaries(case_id=case_id)
    if result.status_code != 200:
        raise SystemExit(f"List summaries failed: {result.status_code}")

    for doc in result.parsed:
        print(f"{doc['filename']}: {(doc.get('summary') or {}).get('document_type')}")
    # docs:end


asyncio.run(main())
