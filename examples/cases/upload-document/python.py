import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case

    with open("invoice_001.pdf", "rb") as f:
        file_data = f.read()

    body = UploadCaseDocumentBody(
        document=File(payload=file_data, file_name="invoice_001.pdf", mime_type="application/pdf")
    )
    response = await client.upload_case_document(case_id=case_id, body=body)
    print(f"Upload status: {response.status_code}")
    # docs:end


asyncio.run(main())
