import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    # Step 1: Create a case first
    case_response = await client.create_case(
        name="Contract Review",
        description="Contract documents for review",
    )
    case_id = case_response.parsed.id

    # Step 2: Upload document to case
    with open("contract.pdf", "rb") as f:
        document_data = f.read()

    upload_body = UploadCaseDocumentBody(
        document=File(payload=document_data, file_name="contract.pdf", mime_type="application/pdf")
    )
    upload_response = await client.upload_case_document(case_id=case_id, body=upload_body)

    print(f"Uploaded document to case {case_id}, status {upload_response.status_code}")
    # docs:end


asyncio.run(main())
