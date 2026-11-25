import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    version_id = 7  # treeVersionId returned when the tree was uploaded

    roots = await client.browse_tree_nodes(case_id, version_id)
    children = await client.browse_tree_nodes(case_id, version_id, parent="4.12")
    hits = await client.browse_tree_nodes(case_id, version_id, q="sock")

    for node in hits["items"]:
        print(node["code"], node["name"], node["status"], node["childCount"])
    print(f"{hits['total']} matches")
    print(f"{len(roots['items'])} roots")
    print(f"{len(children['items'])} children of 4.12")
    # docs:end


asyncio.run(main())
