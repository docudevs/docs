import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    status = await client.agent_chat_and_wait(
        "Classify this week file into levels 4 and 5 using the memo rules.",
        session_id="week-41",
        case_id=42,
        playbook="general",
    )

    print((status.get("response") or {}).get("message"))
    for action in status.get("pendingActions") or []:
        print(action["id"], action["summary"])


asyncio.run(main())
