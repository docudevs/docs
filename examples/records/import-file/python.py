import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with a configured dataset

    # Compresses the file and uploads it in blocks. Raises RecordsImportFailed when the
    # import fails, and TimeoutError after 1800 seconds.
    result = await client.bulk_upsert_records(
        case_id,
        "products.csv",  # columns as named in the dataset field mapping
        wait=True,
        on_progress=print,
    )
    print(result["status"], result["rowsReceived"], result["rowsRejected"])
    print(result["recordsAdded"], result["recordsUpdated"], result["recordsUnchanged"])
    # docs:end


asyncio.run(main())
