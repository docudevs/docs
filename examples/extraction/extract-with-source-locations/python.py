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

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the main clauses from this contract.",
        schema=json.dumps(schema),
        source_locations=True,
        source_location_granularity="block",
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(result, indent=2))

    # The evidence manifest is a separate artifact
    source_locations = await client.get_source_locations(job_id)
    print(json.dumps(source_locations, indent=2))
    # docs:end


asyncio.run(main())
