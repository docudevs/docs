import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # case_id is the id of an organization-wide case.
    case_id = 3

    response = await client.start_case_embedding_reindex(case_id)
    if response.status_code != 200:
        raise SystemExit(f"Reindex failed: {response.status_code} {response.content.decode()}")

    candidate = json.loads(response.content)
    print(f"Candidate generation {candidate['id']}: {candidate['status']}")
    # docs:end


asyncio.run(main())
