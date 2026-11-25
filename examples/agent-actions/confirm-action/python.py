import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    action_id = 9001  # id of a pending action from the case inbox

    confirmed = await client.confirm_agent_action(action_id)
    print(confirmed["status"], confirmed.get("resultStatus"))

    # A write that answered an error ends as "failed", not "confirmed"
    if confirmed["status"] != "confirmed":
        raise SystemExit(f"Action {action_id} {confirmed['status']}: {confirmed.get('result')}")
    # docs:end


asyncio.run(main())
