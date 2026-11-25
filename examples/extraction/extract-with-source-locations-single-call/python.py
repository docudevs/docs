import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    schema = {
        "type": "object",
        "properties": {
            "clauses": {"type": "array", "items": {"type": "string"}},
        },
        "required": ["clauses"],
    }

    with open("contract.pdf", "rb") as f:
        document_data = f.read()

    combined = await client.submit_and_wait_for_document_with_source_locations(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the main clauses from this contract.",
        schema=json.dumps(schema),
        source_location_granularity="block",
        timeout=180,
    )

    print(json.dumps(combined.result, indent=2))
    print(json.dumps(combined.source_locations, indent=2))
    # docs:end


asyncio.run(main())
