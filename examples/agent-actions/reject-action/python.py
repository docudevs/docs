import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    action_id = 9002  # id of a pending action from the case inbox

    rejected = await client.reject_agent_action(action_id, reason="Use the 7.31 subtree instead")
    print(rejected["id"], rejected["status"])
    # docs:end


asyncio.run(main())
