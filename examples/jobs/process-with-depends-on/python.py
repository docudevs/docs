import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient

# docs:start
from docudevs import json_schema
from docudevs.models import UploadCommand
# docs:end


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    header_schema = {
        "type": "object",
        "properties": {
            "invoice_number": {"type": "string"},
            "vendor": {"type": "string"},
            "total": {"type": "number"},
        },
    }

    line_items_schema = {
        "type": "object",
        "properties": {
            "line_items": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "description": {"type": "string"},
                        "amount": {"type": "number"},
                    },
                },
            },
        },
    }

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # First extraction: parse the document and extract the header fields
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice header fields",
        schema=json_schema(header_schema),
    )
    header_result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(header_result, indent=2))

    # Second extraction: reuse the parsed document with another schema (no re-OCR)
    response = await client.process_document(
        guid=job_guid,
        body=UploadCommand(
            mime_type="application/pdf",
            prompt="Extract line items",
            schema=json_schema(line_items_schema),
        ),
        depends_on=job_guid,
    )
    if response.status_code != 200:
        raise RuntimeError(f"HTTP {response.status_code}")

    line_items_result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(line_items_result, indent=2))
    # docs:end


asyncio.run(main())
