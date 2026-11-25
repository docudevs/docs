import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    version_id = 7  # treeVersionId returned when the tree was uploaded

    node = await client.get_tree_node(case_id, version_id, "4.12.3")
    print(" > ".join(node["path"]))
    print([child["code"] for child in node.get("children", [])])
    print([sibling["code"] for sibling in node.get("siblings", [])])
    # docs:end


asyncio.run(main())
