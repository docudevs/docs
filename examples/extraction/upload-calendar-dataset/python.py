import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient

# docs:start
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File
# docs:end


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the knowledge base, from list_knowledge_bases

    with open("public-holidays-2024.csv", "rb") as f:
        file_data = f.read()

    body = UploadCaseDocumentBody(
        document=File(payload=file_data, file_name="public-holidays-2024.csv", mime_type="text/csv")
    )
    response = await client.upload_case_document(case_id=case_id, body=body, dataset_type="calendar")
    print(f"Upload status: {response.status_code}")
    # docs:end


asyncio.run(main())
