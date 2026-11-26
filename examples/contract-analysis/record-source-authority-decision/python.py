import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_source_authority(case_id)
    authority = response.parsed

    # Every guideline document of the case must be listed, with the fingerprint read above.
    # This example confirms each document as policy; set the role of each document.
    documents = [
        {
            "id": source["documentId"],
            "contentFingerprint": source["contentFingerprint"],
            "sourceRole": "policy",
        }
        for source in authority.get("sources", [])
    ]
    if not documents:
        raise SystemExit("The case has no guideline documents that finished processing")

    decision = await client.record_contract_analysis_source_authority_decision(
        case_id,
        observed_revision=authority["revision"],
        decision_type="source_authority",
        content={"documents": documents},
        reason="Reviewer confirmed the governing policy documents",
    )
    if decision.status_code != 200:
        raise SystemExit(f"Source authority was not recorded: {decision.status_code}")
    print(f"Revision: {decision.parsed['revision']}")
    # docs:end


asyncio.run(main())
