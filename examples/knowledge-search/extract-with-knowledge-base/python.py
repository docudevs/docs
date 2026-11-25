import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = "42"  # id of the knowledge base, from list_knowledge_bases

    schema = {
        "type": "object",
        "properties": {
            "records": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "sku": {"type": "string"},
                        "canonical_name": {"type": "string"},
                        "localized_display_name": {"type": "string", "maxLength": 40},
                        "regulatory_class": {"type": "string"},
                        "marketing_summary": {"type": "string"},
                    },
                    "required": ["sku", "canonical_name", "localized_display_name"],
                },
            }
        },
        "required": ["records"],
    }

    tool = {"type": "KNOWLEDGE_BASE_SEARCH", "config": {"caseId": case_id, "topK": 8}}

    with open("products.csv", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="text/csv",
        prompt=(
            "Fill canonical_name, localized_display_name (at most 40 characters), "
            "regulatory_class and marketing_summary for every SKU, "
            "using the Signature Appliances knowledge base."
        ),
        schema=json.dumps(schema),
        tools=[tool],
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
