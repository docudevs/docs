import asyncio
import json
import os

# docs:start
from pydantic import BaseModel

from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    class ContractClauses(BaseModel):
        clauses: list[str]

    with open("contract.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the main clauses from this contract.",
        schema=json_schema(ContractClauses),
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
