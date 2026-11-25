import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    record = await client.get_record(case_id, "1000000000001")
    print(record.get("label"), record.get("labelPath", []), record.get("labelSource"))
    print(record.get("chunk"))
    # docs:end


asyncio.run(main())
