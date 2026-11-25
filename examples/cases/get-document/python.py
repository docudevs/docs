import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    document_id = "doc-uuid-here"  # documentId from list_case_documents

    response = await client.get_case_document(case_id, document_id)
    document = response.parsed

    print(f"Document: {document.filename}")
    print(f"Content Type: {document.content_type}")
    print(f"Processing Status: {document.processing_status}")
    print(f"Metadata: {document.metadata}")
    # docs:end


asyncio.run(main())
