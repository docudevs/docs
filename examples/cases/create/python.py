import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    response = await client.create_case(
        name="Q4 2024 Vendor Invoices",
        description="All invoices from vendors for Q4 2024 processing",
    )
    if response.status_code != 201:
        raise SystemExit(f"Create case failed: {response.status_code}")

    case = response.parsed
    print(f"Created case ID: {case.id}")


asyncio.run(main())
