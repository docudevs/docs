import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    import_id = 301  # id of the import that failed

    # Raises RecordsApiError (409) when the import has not failed
    accepted = await client.retry_import(case_id, import_id)
    print(f"Import {accepted['importId']}: {accepted['status']}")
    # docs:end


asyncio.run(main())
