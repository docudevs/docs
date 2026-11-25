import asyncio
import os
from typing import List

# docs:start
from pydantic import BaseModel, Field

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # 1. Define your data model
    class LineItem(BaseModel):
        description: str
        quantity: int
        price: float

    class Invoice(BaseModel):
        number: str = Field(description="The invoice number")
        date: str
        items: List[LineItem]
        total: float

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # 2. Generate the schema from the model and process the document
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        schema=json_schema(Invoice),
    )

    # 3. Validate the result back into the model
    result_dict = await client.wait_until_ready(job_id, result_format="json")
    invoice = Invoice(**result_dict)

    print(f"Invoice {invoice.number} total: {invoice.total}")
    # docs:end


asyncio.run(main())
