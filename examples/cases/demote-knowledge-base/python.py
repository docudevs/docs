import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a knowledge base

    response = await client.delete_knowledge_base(case_id)
    if response.status_code >= 300:
        raise SystemExit(f"Demote failed: {response.status_code}")

    print(f"Case {case_id} is no longer a knowledge base")
    # docs:end


asyncio.run(main())
