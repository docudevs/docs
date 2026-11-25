import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    status = await client.agent_chat_and_wait(
        [{"role": "user", "content": "Record that fiction books are filed by genre when the description names one."}],
        session_id="run-9001-reply",
        case_id=42,
        playbook="general",
        context="run:9001",  # run:{runId}, the id returned when the run was started
    )

    print((status.get("response") or {}).get("message"))
    for action in status.get("pendingActions") or []:
        print(action["id"], action["summary"])
    # docs:end


asyncio.run(main())
