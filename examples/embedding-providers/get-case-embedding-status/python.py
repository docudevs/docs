import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # case_id is the id of an organization-wide case.
    case_id = 3

    response = await client.get_case_embedding_status(case_id)
    if response.status_code != 200:
        raise SystemExit(f"Status failed: {response.status_code}")

    status = json.loads(response.content)
    active = status.get("active")
    candidate = status.get("candidate")
    print(f"Active dimensions: {active['dimensions'] if active else None}")
    print(f"Candidate status: {candidate['status'] if candidate else None}")
    # docs:end


asyncio.run(main())
