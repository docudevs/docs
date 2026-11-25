import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = "51"  # id of the records knowledge base, from list_knowledge_bases

    schema = {
        "type": "object",
        "properties": {
            "lines": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "description": {"type": "string"},
                        "product_key": {"type": "string"},
                    },
                },
            }
        },
    }

    tool = {"type": "RECORDS_SEARCH", "config": {"caseId": case_id, "topK": 5}}

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="For each invoice line, fill product_key with the catalogue record that matches the item.",
        schema=json.dumps(schema),
        tools=[tool],
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
