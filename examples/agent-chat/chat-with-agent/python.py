import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # Sends the message, polls the status endpoint and raises TimeoutError after 120 seconds
    status = await client.agent_chat_and_wait(
        messages=[{"role": "user", "content": "Help me build an invoice extraction schema"}],
        session_id="session-123",
    )

    reply = status.get("response") or {}
    print(reply.get("message"))


asyncio.run(main())
