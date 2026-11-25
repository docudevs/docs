import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents

    response = await client.get_contract_analysis_trial_readiness(case_id)
    readiness = response.parsed

    print(f"Ready: {readiness['ready']}")
    print(f"Blockers: {readiness.get('blockers', [])}")
    print(f"Scenarios still missing: {readiness.get('outstandingScenarios', [])}")
    # docs:end


asyncio.run(main())
