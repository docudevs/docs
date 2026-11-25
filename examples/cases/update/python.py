import asyncio
import os

# docs:start
from docudevs.models import CasesControllerUpdateCaseRequest
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    update_request = CasesControllerUpdateCaseRequest(
        name="Q4 2024 Vendor Invoices - Updated",
        description="Updated description with additional context",
    )

    response = await client.update_case(case_id, update_request)
    if response.status_code != 200:
        raise SystemExit(f"Update case failed: {response.status_code}")

    updated_case = response.parsed
    print(f"Updated case: {updated_case.name}")
    # docs:end


asyncio.run(main())
