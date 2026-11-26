import asyncio
import os

# docs:start
from pydantic import BaseModel

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    batch_guid = await client.create_batch(max_concurrency=3)

    for filename in ["invoice1.pdf", "invoice2.pdf", "invoice3.pdf"]:
        with open(filename, "rb") as f:
            await client.upload_batch_document(
                batch_guid=batch_guid,
                document=f.read(),
                mime_type="application/pdf",
                file_name=filename,
            )

    class Invoice(BaseModel):
        invoice_number: str | None = None
        total: float

    await client.process_batch(
        batch_guid=batch_guid,
        mime_type="application/pdf",
        prompt="Extract invoice data",
        schema=json_schema(Invoice),
    )

    results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
    for i, result in enumerate(results):
        if result is None:
            print(f"Document {i}: no result (it failed or has no stored result)")
        else:
            print(f"Document {i}: {result}")
    # docs:end


asyncio.run(main())
