import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case

    versions = await client.list_trees(case_id)
    for version in versions.get("items", []):
        print(version["treeVersionId"], version["status"], version["nodes"], version["levels"])
    # docs:end


asyncio.run(main())
