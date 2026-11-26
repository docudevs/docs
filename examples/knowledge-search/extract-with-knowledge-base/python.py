import asyncio
import json
import os

# docs:start
from pydantic import BaseModel, Field

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = "42"  # id of the knowledge base, from list_knowledge_bases

    class ProductRecord(BaseModel):
        sku: str
        canonical_name: str
        localized_display_name: str = Field(max_length=40)
        product_group: str | None = None
        marketing_summary: str | None = None

    class ProductRecords(BaseModel):
        records: list[ProductRecord]

    tool = {"type": "KNOWLEDGE_BASE_SEARCH", "config": {"caseId": case_id, "topK": 8}}

    with open("products.csv", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="text/csv",
        prompt=(
            "Fill canonical_name, localized_display_name (at most 40 characters), "
            "product_group and marketing_summary for every SKU, "
            "using the Product Catalogue knowledge base."
        ),
        schema=json_schema(ProductRecords),
        tools=[tool],
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
