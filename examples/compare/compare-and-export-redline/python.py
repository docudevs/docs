import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    compare_guid = await client.compare_documents(
        "slip-2025.pdf",
        "slip-2026-draft.pdf",
        original_label="Slip 2025",
        revised_label="Draft 2026",
        ocr="PREMIUM",
    )

    # Raises an error that names the stage if a stage fails, and TimeoutError after 1200 seconds
    artifact = await client.wait_until_compare_ready(compare_guid, timeout=1200)
    print(f"Changes found: {len(artifact.get('changes', []))}")

    # Export a redline as PDF with only the substantive changes
    pdf_bytes = await client.export_document_compare(
        compare_guid,
        "pdf",
        filters={"substantiveOnly": True},
        save_to="redline.pdf",
    )
    print(f"Saved {len(pdf_bytes)} bytes to redline.pdf")
    # docs:end


asyncio.run(main())
