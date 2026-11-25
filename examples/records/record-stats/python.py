import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    stats = await client.record_stats(case_id)
    print(f"{stats['records']} records, {stats['unlabelled']} without a label")
    for code, counts in stats["labels"].items():
        print(code, counts)
    # docs:end


asyncio.run(main())
