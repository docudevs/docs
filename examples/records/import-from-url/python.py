import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with a configured dataset

    # The worker fetches the file, so the URL must be https and publicly reachable
    result = await client.import_records_from_url(
        case_id,
        "https://example.com/exports/products.csv",
        format="csv",
        wait=True,
    )
    print(result["status"], result["rowsReceived"], result["recordsAdded"])
    # docs:end


asyncio.run(main())
