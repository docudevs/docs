import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id returned by create_case

    pending = await client.list_agent_actions(case_id, status="pending", limit=20)
    for action in pending.get("items") or []:
        print(action["id"], action["tool"], action["summary"])


asyncio.run(main())
