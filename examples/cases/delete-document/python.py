import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    document_id = "doc-uuid-here"  # documentId from list_case_documents

    response = await client.delete_case_document(case_id, document_id)

    if response.status_code == 200:
        print("Document deleted successfully")
    # docs:end


asyncio.run(main())
