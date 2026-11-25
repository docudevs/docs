import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    draft_revision = 2  # draftRevision returned by the last draft update
    acknowledged_warning_codes = []  # List here the warning codes from the readiness or publish response that you accept.

    published = await client.publish_contract_analysis_profile(
        case_id,
        draft_revision=draft_revision,
        acknowledged_warning_codes=acknowledged_warning_codes,
    )
    print(f"Published revision: {published.parsed['publishedRevision']}")
    # docs:end


asyncio.run(main())
