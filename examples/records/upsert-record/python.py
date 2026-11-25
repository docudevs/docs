import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    # Raises RecordsApiError when the request is refused
    accepted = await client.upsert_record(
        case_id,
        "6410000123456",
        {"title": "Soft sock 3-pack", "description": "Cotton, size 40-45"},
        label="4.12.3",
        metadata={"brand": "Foo"},
    )
    print(f"Import {accepted['importId']}: {accepted['status']}")
    # docs:end


asyncio.run(main())
