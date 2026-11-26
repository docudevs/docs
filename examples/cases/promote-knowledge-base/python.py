import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of an organization-wide case, created with an API key

    response = await client.promote_knowledge_base(case_id)
    if response.status_code != 201:
        raise SystemExit(f"Promote failed: {response.status_code}")

    case = response.parsed
    print(f"Case {case['id']} ({case['name']}): knowledge base = {case['isKnowledgeBase']}")
    # docs:end


asyncio.run(main())
