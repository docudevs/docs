import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    draft_revision = 2  # draftRevision of the draft the trial ran against
    attestation_id = "<attestation-id>"  # attestationId returned by the attest request

    published = await client.publish_contract_analysis_profile(
        case_id,
        draft_revision=draft_revision,
        trial_attestation_id=attestation_id,
    )
    print(f"Published revision: {published.parsed['publishedRevision']}")
    # docs:end


asyncio.run(main())
