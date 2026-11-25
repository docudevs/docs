import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_profile(case_id, view="draft")
    draft = response.parsed

    profile = draft["profile"]  # edit the profile here

    updated = await client.update_contract_analysis_draft(
        case_id,
        draft_revision=draft["draftRevision"],
        profile=profile,
        reason="Reviewed clause wording",
    )
    print(f"Draft revision: {updated.parsed['draftRevision']}")
    # docs:end


asyncio.run(main())
