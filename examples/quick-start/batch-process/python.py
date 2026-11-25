import asyncio
import os

from docudevs import json_schema
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

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

    schema = {
        "type": "object",
        "properties": {
            "invoice_number": {"type": "string"},
            "total": {"type": "number"},
        },
    }

    await client.process_batch(
        batch_guid=batch_guid,
        mime_type="application/pdf",
        prompt="Extract invoice data",
        schema=json_schema(schema),
    )

    results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
    for i, result in enumerate(results):
        print(f"Document {i}: {result}")
    # docs:end


asyncio.run(main())
