import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents

    generation = await client.generate_contract_analysis_profile(
        case_id,
        our_party="Acme Corp",
        counterparty="Globex Ltd",
        scope={"jurisdictions": ["Finland"], "from": "1 January 2026"},
    )
    print(f"Profile generation job: {generation.parsed['jobGuid']}")
    # docs:end


asyncio.run(main())
