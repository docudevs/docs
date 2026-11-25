import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents

    suggestion = await client.suggest_source_roles(case_id)
    print(f"Suggestion job: {suggestion.parsed['jobGuid']}")
    # docs:end


asyncio.run(main())
