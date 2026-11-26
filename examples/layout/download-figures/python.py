import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("report.pdf", "rb") as f:
        document_data = f.read()

    # extract_figures keeps the figures as images; PREMIUM OCR also renders page images
    job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown",
        extract_figures=True,
    )
    await client.wait_until_ready(job_id, timeout=600)

    # List the figures, then download each one
    metadata = await client.get_figures_metadata(job_id)
    images = (metadata or {}).get("images", [])
    for image in images:
        page_index = image.get("pageIndex")
        print(f"{image['id']} page {page_index if page_index is not None else 'unknown'}")

    for image in images:
        figure_bytes = await client.get_figure_image(job_id, image["id"])
        if figure_bytes:
            with open(f"{image['id']}.png", "wb") as f:
                f.write(figure_bytes)

    # Download the image of the first page (page indexes start at 0)
    page_bytes = await client.get_image(job_id, 0)
    if page_bytes:
        with open("page_0.png", "wb") as f:
            f.write(page_bytes)
    # docs:end


asyncio.run(main())
