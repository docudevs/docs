import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    version_id = 7  # treeVersionId returned when the tree was uploaded

    # Raises RecordsImportFailed when the job fails, and TimeoutError after 3600 seconds
    job = await client.activate_tree(
        case_id,
        version_id,
        successor_mapping={"9.90.1": "4.12.3"},
        allow_missing=False,
        wait=True,
    )
    print(f"Activation job {job['jobGuid']}: {job['status']}")
    # docs:end


asyncio.run(main())
