import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed job processed with figure extraction

    selection = await client.submit_and_wait_for_image_selection(
        job_guid,
        prompt="Return all diagrams from the document",
        top_k=5,
        match_mode="all",
        use_vision=False,
    )

    payload = json.loads(selection.result)
    print(json.dumps(payload, indent=2))

    selected = payload.get("selected", [])
    if selected:
        image_bytes = await client.get_figure_image(job_guid, selected[0]["id"])
        if image_bytes:
            with open("selected_figure.png", "wb") as f:
                f.write(image_bytes)
    # docs:end


asyncio.run(main())
