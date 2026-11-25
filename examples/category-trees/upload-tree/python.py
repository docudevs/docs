import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient, RecordsApiError


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    nodes = [
        {"code": "4", "parent": None, "name": "Clothing"},
        {"code": "4.12", "parent": "4", "name": "Socks and tights"},
        {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
        {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"},
    ]

    try:
        created = await client.upload_tree(case_id, nodes, name="Catalogue 2026-10")
    except RecordsApiError as error:
        # A rejected tree stores nothing; its body lists every problem
        for problem in error.body.get("problems", []):
            print(problem["type"], problem.get("code"), problem["message"])
        raise
    print(f"Tree version {created['treeVersionId']}: {created['status']}, {created['nodes']} nodes")


asyncio.run(main())
