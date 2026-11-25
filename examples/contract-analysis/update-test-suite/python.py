import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_test_suite(case_id)
    suite = response.parsed

    cases = suite.get("cases", [])  # add or edit suite cases here

    updated = await client.update_contract_analysis_test_suite(
        case_id,
        revision=suite["revision"],
        cases=cases,
    )
    print(f"Suite revision: {updated.parsed['revision']}")
    # docs:end


asyncio.run(main())
