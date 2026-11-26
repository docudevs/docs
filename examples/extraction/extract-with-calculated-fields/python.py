import asyncio
import os

# docs:start
from pydantic import BaseModel, Field

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    class Invoice(BaseModel):
        net_amount: float | None = None
        vat_rate: float | None = None
        # Calculated fields are removed from the schema before extraction, so this one may be required
        gross_amount: float = Field(
            json_schema_extra={
                "x-docudevs": {
                    "fieldType": "calculated",
                    "dependsOn": ["net_amount", "vat_rate"],
                    "formula": {
                        "lang": "excel",
                        "expression": "ROUND({net_amount} * (1 + {vat_rate}), 2)",
                    },
                },
            },
        )

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract net amount and vat rate as numbers.",
        schema=json_schema(Invoice),
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(result["gross_amount"])
    # docs:end


asyncio.run(main())
