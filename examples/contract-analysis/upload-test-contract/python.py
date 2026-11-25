import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of the case that holds the playbook documents

    with open("supplier-agreement.pdf", "rb") as source:
        file_data = source.read()

    body = UploadCaseDocumentBody(
        document=File(payload=file_data, file_name="supplier-agreement.pdf", mime_type="application/pdf")
    )
    response = await client.upload_case_document(
        case_id,
        body,
        dataset_type="test_contract",
        test_contract_retention="kept",
    )
    print(f"Upload status: {response.status_code}")


asyncio.run(main())
