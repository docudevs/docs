import asyncio
import json
import os

# docs:start
import datetime

from pydantic import BaseModel

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    class Invoice(BaseModel):
        invoice_number: str
        date: datetime.date | None = None
        vendor: str | None = None
        total: float
        currency: str | None = None

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice data according to the schema",
        schema=json_schema(Invoice),
    )

    data = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(data, indent=2))
    # docs:end


asyncio.run(main())
