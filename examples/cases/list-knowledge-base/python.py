import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.list_knowledge_bases()
    if response.status_code != 200:
        raise SystemExit(f"List knowledge bases failed: {response.status_code}")

    for knowledge_base in response.parsed or []:
        print(f"Knowledge base {knowledge_base['id']}: {knowledge_base['name']}")
    # docs:end


asyncio.run(main())
