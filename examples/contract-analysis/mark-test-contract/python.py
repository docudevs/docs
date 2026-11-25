import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    document_id = "<document-id>"  # id of a completed document in the case

    response = await client.update_case_document(
        case_id,
        document_id,
        dataset_type="test_contract",
        test_contract_retention="kept",
    )
    print(f"Update status: {response.status_code}")
    # docs:end


asyncio.run(main())
