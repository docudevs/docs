import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    import_id = 301  # importId returned when the import was created

    # Raises RecordsImportFailed when the import fails, and TimeoutError after 1800 seconds
    finished = await client.wait_for_import(case_id, import_id, on_progress=print)
    print(finished["status"], finished["recordsAdded"], finished["recordsUpdated"])
    # docs:end


asyncio.run(main())
