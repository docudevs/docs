import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient

# docs:start
import datetime

from pydantic import BaseModel

from docudevs import json_schema
# docs:end


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    class WarrantyLineItem(BaseModel):
        description: str | None = None
        warranty_end_date: datetime.date | None = None
        remaining_warranty_days: float | None = None

    class WarrantyList(BaseModel):
        line_items: list[WarrantyLineItem] | None = None

    with open("warranty-list.pdf", "rb") as f:
        document_data = f.read()

    # Every chunk receives the current date
    job_guid = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract line items with remaining warranty days",
        schema=json_schema(WarrantyList),
        pages_per_chunk=2,
        include_current_date=True,
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
