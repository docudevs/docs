import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    import_id = 301  # importId returned when the import was created

    current = await client.get_import(case_id, import_id)
    print(current["status"], current["rowsReceived"], current["rowsRejected"])
    print(current["recordsAdded"], current["recordsUpdated"], current["recordsDeleted"])
    # docs:end


asyncio.run(main())
