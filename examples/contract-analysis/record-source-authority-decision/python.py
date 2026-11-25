import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    response = await client.get_contract_analysis_source_authority(case_id)
    authority = response.parsed

    # "policy-doc" in the decision below must be the id of a real source document in the case
    decision = await client.record_contract_analysis_source_authority_decision(
        case_id,
        observed_revision=authority["revision"],
        decision_type="source_authority",
        content={
            "documents": [
                {"id": "policy-doc", "sourceRole": "policy", "excluded": False}
            ]
        },
        reason="Reviewer confirmed the governing policy document",
    )
    print(f"Revision: {decision.parsed['revision']}")
    # docs:end


asyncio.run(main())
