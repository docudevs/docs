import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case

    response = await client.list_case_documents(case_id=case_id, page=0, size=20)
    documents_page = response.parsed

    print(f"Total documents: {documents_page.total_size}")
    print(f"Page: {documents_page.page_number}/{documents_page.total_pages}")

    for document in documents_page.content:
        print(f"{document.filename} [{document.document_id}]")
        print(f"  status={document.processing_status}")
        print(f"  size={document.size_bytes} bytes, uploaded={document.created_at}")
    # docs:end


asyncio.run(main())
