import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 123  # id returned by create_case
    document_id = "doc-uuid-here"  # documentId from list_case_documents

    result = await client.get_document_summary(case_id=case_id, document_id=document_id)
    summary = result.parsed
    print(summary["filename"])  # "invoice-q4.pdf"
    print(summary["summary"]["document_type"])  # "invoice"
    print(summary["summary"]["summary"])  # "Monthly invoice from Acme Corp..."
    # docs:end


asyncio.run(main())
