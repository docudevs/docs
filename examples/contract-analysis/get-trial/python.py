import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    trial_id = "<trial-id>"  # trialId returned when the trial was launched

    response = await client.get_contract_analysis_trial(case_id, trial_id)
    trial = response.parsed

    print(f"Status: {trial['status']}")
    # docs:end


asyncio.run(main())
