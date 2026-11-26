import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    status = await client.agent_chat_and_wait(
        "Investigate the flagged rows of classify job 55.",
        session_id="week-41",
        case_id=42,
        playbook="investigate-flagged",
        context="classify:55:17",
    )

    print((status.get("response") or {}).get("message"))
    for action in status.get("pendingActions") or []:
        print(action["id"], action["summary"])
    # docs:end


asyncio.run(main())
