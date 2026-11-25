import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    trial_id = "<trial-id>"  # trialId returned when the trial was launched

    trial = (await client.get_contract_analysis_trial(case_id, trial_id)).parsed

    if not trial.get("report"):
        raise SystemExit("The trial has no report yet")

    attested = await client.attest_contract_analysis_trial(
        case_id,
        trial_id,
        report_id=trial["report"]["reportId"],
        reason="Reviewed and correct",
    )
    print(f"Attestation ID: {attested.parsed['attestationId']}")
    # docs:end


asyncio.run(main())
