import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = None
    try:
        response = await client.create_case(name="Contract Review")
        if response.status_code != 201:
            raise RuntimeError(f"HTTP {response.status_code}")
        case_id = response.parsed.id
        print(f"Created case: {case_id}")
    except Exception as e:
        print(f"Error creating case: {e}")
        # Handle error appropriately

    if case_id is not None:
        try:
            with open("contract.pdf", "rb") as f:
                document_data = f.read()

            body = UploadCaseDocumentBody(
                document=File(payload=document_data, file_name="contract.pdf", mime_type="application/pdf")
            )
            response = await client.upload_case_document(case_id=case_id, body=body)
            if response.status_code != 201:
                raise RuntimeError(f"HTTP {response.status_code}")
            print("Uploaded document to case")
        except Exception as e:
            print(f"Error uploading document: {e}")
            # Handle error appropriately
    # docs:end


asyncio.run(main())
