import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("large-invoice.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        # Instructions for extracting the rows (line items)
        prompt="Extract invoice line items (sku, description, quantity, unitPrice, total).",
        # Chunking strategy
        pages_per_chunk=4,
        overlap_pages=1,
        dedup_key="sku",  # unique key used to remove rows repeated by the overlap
        parallel_processing=True,
        # Header extraction strategy
        header_options={
            "page_limit": 1,  # look for the header on page 1 only
            "include_in_rows": False,  # do not process page 1 as rows
            "row_prompt_augmentation": "This is an invoice from Acme Corp.",
        },
        header_schema=(
            '{"type": "object", "properties": {"invoiceNumber": {"type": "string"},'
            ' "issueDate": {"type": "string"}, "billingAddress": {"type": "string"}}}'
        ),
    )
    print(f"Job submitted: {job_id}")

    result = await client.wait_until_ready(job_id, result_format="json")
    print("Header:", result.get("header"))
    print(f"Extracted {len(result.get('records', []))} line items.")
    # docs:end


asyncio.run(main())
